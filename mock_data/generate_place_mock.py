#!/usr/bin/env python3
"""Deterministic Place performance-test data generator and MySQL bulk loader.

The tool is intentionally separate from the Spring application. It creates
only rows for the place table and uses LOAD DATA LOCAL INFILE instead of
millions of INSERT statements.
"""

from __future__ import annotations

import argparse
import csv
import datetime as dt
import hashlib
import json
import os
import random
import re
import shutil
import subprocess
import sys
import tempfile
import time
from dataclasses import dataclass
from pathlib import Path
from typing import Any, Iterable, Mapping, Sequence


SCRIPT_DIR = Path(__file__).resolve().parent
DEFAULT_CONFIG_PATH = SCRIPT_DIR / "place_mock_config.json"
MANIFEST_VERSION = 2
MYSQL_SIGNED_INT_MAX = 2_147_483_647
PLACE_TABLE = "place"
PLACE_LOAD_COLUMNS = (
    "content_id",
    "content_type_id",
    "name",
    "address",
    "latitude",
    "longitude",
    "description",
    "src_created_at",
    "src_updated_at",
)
IDENTIFIER_RE = re.compile(r"^[A-Za-z0-9_$]+$")


class GeneratorError(RuntimeError):
    """Expected, user-actionable generator failure."""


@dataclass(frozen=True)
class DatasetSettings:
    config_path: Path
    config: Mapping[str, Any]
    config_hash: str
    seed: int
    profile: str
    count: int
    chunk_size: int
    output_dir: Path
    content_id_namespace: str
    content_id_start: int

    @property
    def place_config(self) -> Mapping[str, Any]:
        return self.config["place"]

    @property
    def chunks(self) -> int:
        return (self.count + self.chunk_size - 1) // self.chunk_size

    @property
    def content_id_end(self) -> int:
        return self.content_id_start + self.count - 1

    @property
    def manifest_path(self) -> Path:
        return self.output_dir / f"{self.content_id_namespace}.manifest.json"

    @property
    def fingerprint(self) -> str:
        payload = {
            "config_hash": self.config_hash,
            "seed": self.seed,
            "profile": self.profile,
            "count": self.count,
            "chunk_size": self.chunk_size,
            "content_id_namespace": self.content_id_namespace,
            "content_id_start": self.content_id_start,
            "content_id_end": self.content_id_end,
        }
        return hashlib.sha256(
            json.dumps(payload, sort_keys=True, separators=(",", ":")).encode("utf-8")
        ).hexdigest()


@dataclass(frozen=True)
class DatabaseOptions:
    database: str
    host: str
    port: int
    user: str
    password_env: str
    env_file: Path | None
    mysql_bin: str


def fail(message: str) -> None:
    raise GeneratorError(message)


def read_json(path: Path) -> Mapping[str, Any]:
    try:
        with path.open("r", encoding="utf-8") as stream:
            value = json.load(stream)
    except FileNotFoundError:
        fail(f"설정 파일을 찾을 수 없습니다: {path}")
    except json.JSONDecodeError as exc:
        fail(f"설정 파일 JSON 문법이 올바르지 않습니다: {path} ({exc})")
    if not isinstance(value, dict):
        fail("설정 파일 최상위 값은 object여야 합니다.")
    return value


def canonical_hash(value: Mapping[str, Any]) -> str:
    encoded = json.dumps(
        value, ensure_ascii=False, sort_keys=True, separators=(",", ":")
    ).encode("utf-8")
    return hashlib.sha256(encoded).hexdigest()


def resolve_path(raw_path: str | Path, base_dir: Path) -> Path:
    path = Path(raw_path)
    if not path.is_absolute():
        path = base_dir / path
    return path.resolve()


def validate_bounds(bounds: Sequence[Any], label: str) -> tuple[float, float, float, float]:
    if not isinstance(bounds, (list, tuple)) or len(bounds) != 4:
        fail(f"{label}는 [최소위도, 최대위도, 최소경도, 최대경도] 4개 값이어야 합니다.")
    values = tuple(float(value) for value in bounds)
    min_lat, max_lat, min_lon, max_lon = values
    if not (min_lat < max_lat and min_lon < max_lon):
        fail(f"{label}의 최소/최대 관계가 올바르지 않습니다: {values}")
    return values


def validate_weighted_entries(
    entries: Any, label: str, value_key: str = "value"
) -> None:
    if not isinstance(entries, list) or not entries:
        fail(f"{label}은(는) 비어 있지 않은 배열이어야 합니다.")
    total = 0.0
    for index, entry in enumerate(entries):
        if isinstance(entry, str):
            weight = 1.0
        elif isinstance(entry, dict):
            if value_key not in entry:
                fail(f"{label}[{index}]에 {value_key}가 없습니다.")
            weight = float(entry.get("weight", 1.0))
        else:
            fail(f"{label}[{index}]은 문자열 또는 object여야 합니다.")
        if weight <= 0:
            fail(f"{label}[{index}]의 weight는 0보다 커야 합니다.")
        total += weight
    if total <= 0:
        fail(f"{label}의 weight 합이 0입니다.")


def validate_config(config: Mapping[str, Any]) -> None:
    if not isinstance(config.get("seed"), int) or config["seed"] < 0:
        fail("seed는 0 이상의 정수여야 합니다.")

    place = config.get("place")
    if not isinstance(place, dict):
        fail("place 설정이 필요합니다.")

    profiles = place.get("profiles")
    if not isinstance(profiles, dict) or not profiles:
        fail("place.profiles 설정이 필요합니다.")
    for profile_name, count in profiles.items():
        if not isinstance(profile_name, str) or not isinstance(count, int) or count <= 0:
            fail(f"place.profiles.{profile_name}은(는) 0보다 큰 정수여야 합니다.")

    configured_chunk_size = place.get("chunk_size")
    if not isinstance(configured_chunk_size, int) or configured_chunk_size <= 0:
        fail("place.chunk_size는 0보다 큰 정수여야 합니다.")

    content_id_config = place.get("content_id")
    if not isinstance(content_id_config, dict):
        fail("place.content_id configuration is required.")
    content_id_base = content_id_config.get("base")
    namespace_slots = content_id_config.get("namespace_slots")
    namespace_capacity = content_id_config.get("namespace_capacity")
    if not all(
        isinstance(value, int) and not isinstance(value, bool)
        for value in (content_id_base, namespace_slots, namespace_capacity)
    ):
        fail("place.content_id base/namespace_slots/namespace_capacity must be integers.")
    if content_id_base <= 0 or namespace_slots <= 0 or namespace_capacity <= 0:
        fail("place.content_id numeric settings must be positive.")
    numeric_namespace_end = content_id_base + namespace_slots * namespace_capacity - 1
    if numeric_namespace_end > MYSQL_SIGNED_INT_MAX:
        fail("place.content_id numeric namespace exceeds signed MySQL INT range.")
    if max(profiles.values()) > namespace_capacity:
        fail("a configured profile count exceeds content_id namespace_capacity.")

    content_types = place.get("content_types")
    if not isinstance(content_types, list) or not content_types:
        fail("place.content_types 설정이 필요합니다.")
    ids: set[int] = set()
    type_weight = 0.0
    for index, content_type in enumerate(content_types):
        if not isinstance(content_type, dict):
            fail(f"place.content_types[{index}]은 object여야 합니다.")
        content_type_id = content_type.get("id")
        if not isinstance(content_type_id, int) or content_type_id <= 0:
            fail(f"place.content_types[{index}].id는 양의 정수여야 합니다.")
        if content_type_id in ids:
            fail(f"content_type_id가 중복됩니다: {content_type_id}")
        ids.add(content_type_id)
        if not isinstance(content_type.get("name"), str) or not content_type["name"]:
            fail(f"place.content_types[{index}].name이 필요합니다.")
        weight = float(content_type.get("weight", 0))
        if weight <= 0:
            fail(f"place.content_types[{index}].weight는 0보다 커야 합니다.")
        type_weight += weight
        validate_weighted_entries(
            content_type.get("suffixes"),
            f"place.content_types[{index}].suffixes",
        )
    if type_weight <= 0:
        fail("content type weight 합이 0입니다.")

    regions = place.get("regions")
    if not isinstance(regions, list) or not regions:
        fail("place.regions 설정이 필요합니다.")
    global_bounds = validate_bounds(place.get("korea_bounds"), "place.korea_bounds")
    region_weight = 0.0
    for index, region in enumerate(regions):
        if not isinstance(region, dict):
            fail(f"place.regions[{index}]은 object여야 합니다.")
        if not region.get("name") or not region.get("address_prefixes"):
            fail(f"place.regions[{index}]에 name/address_prefixes가 필요합니다.")
        bounds = validate_bounds(region.get("bounds"), f"place.regions[{index}]")
        if not (
            global_bounds[0] <= bounds[0] <= bounds[1] <= global_bounds[1]
            and global_bounds[2] <= bounds[2] <= bounds[3] <= global_bounds[3]
        ):
            fail(f"place.regions[{index}].bounds가 korea_bounds 밖에 있습니다.")
        weight = float(region.get("weight", 0))
        if weight <= 0:
            fail(f"place.regions[{index}].weight는 0보다 커야 합니다.")
        region_weight += weight
        hotspots = region.get("hotspots")
        validate_weighted_entries(hotspots, f"place.regions[{index}].hotspots", "name")
        for hotspot_index, hotspot in enumerate(hotspots):
            if not isinstance(hotspot, dict):
                fail(f"hotspot[{hotspot_index}]은 object여야 합니다.")
            required = ("city", "district", "neighborhood", "center", "sigma")
            if any(not hotspot.get(key) for key in required):
                fail("hotspot에 city/district/neighborhood/center/sigma가 필요합니다.")
            center = hotspot["center"]
            sigma = hotspot["sigma"]
            if not isinstance(center, list) or len(center) != 2:
                fail(f"hotspot center는 [위도, 경도]여야 합니다: {hotspot}")
            if not isinstance(sigma, list) or len(sigma) != 2:
                fail("hotspot sigma는 [위도표준편차, 경도표준편차]여야 합니다.")
            if float(sigma[0]) <= 0 or float(sigma[1]) <= 0:
                fail(f"hotspot sigma는 양수여야 합니다: {hotspot}")
            lat, lon = float(center[0]), float(center[1])
            if not (bounds[0] <= lat <= bounds[1] and bounds[2] <= lon <= bounds[3]):
                fail(f"hotspot 중심이 지역 bounds 밖에 있습니다: {hotspot}")
            address_variants = hotspot.get("address_variants")
            if address_variants is not None:
                validate_weighted_entries(
                    address_variants,
                    f"place.regions[{index}].hotspots[{hotspot_index}].address_variants",
                    "neighborhood",
                )
                for variant_index, variant in enumerate(address_variants):
                    if not isinstance(variant, dict):
                        fail("address_variants 항목은 object여야 합니다.")
                    for key in ("city", "district", "neighborhood"):
                        if key in variant and (
                            not isinstance(variant[key], str) or not variant[key]
                        ):
                            fail(
                                f"address_variants[{variant_index}].{key}는 비어 있지 않은 문자열이어야 합니다."
                            )
    if region_weight <= 0:
        fail("region weight 합이 0입니다.")

    name_config = place.get("name")
    if not isinstance(name_config, dict):
        fail("place.name 설정이 필요합니다.")
    validate_weighted_entries(name_config.get("prefixes"), "place.name.prefixes")
    validate_weighted_entries(name_config.get("forms"), "place.name.forms", "template")
    rare = name_config.get("rare_token")
    if not isinstance(rare, dict) or not rare.get("token"):
        fail("place.name.rare_token 설정이 올바르지 않습니다.")
    if float(rare.get("probability", 0)) < 0:
        fail("rare_token probability는 음수가 될 수 없습니다.")

    description = place.get("description")
    if not isinstance(description, dict):
        fail("place.description 설정이 필요합니다.")
    buckets = description.get("length_buckets")
    if not isinstance(buckets, list) or not buckets:
        fail("place.description.length_buckets 설정이 필요합니다.")
    probability_sum = 0.0
    for bucket in buckets:
        if not isinstance(bucket, dict) or not bucket.get("kind"):
            fail("description length bucket은 kind를 가져야 합니다.")
        probability = float(bucket.get("probability", 0))
        if probability < 0:
            fail("description bucket probability는 음수가 될 수 없습니다.")
        probability_sum += probability
        if bucket["kind"] != "null":
            minimum = int(bucket.get("min", 0))
            maximum = int(bucket.get("max", 0))
            if minimum <= 0 or minimum > maximum:
                fail(f"description bucket 길이 범위가 올바르지 않습니다: {bucket}")
    if abs(probability_sum - 1.0) > 0.000001:
        fail(f"description bucket probability 합은 1이어야 합니다: {probability_sum}")

    date_config = place.get("dates")
    if not isinstance(date_config, dict):
        fail("place.dates 설정이 필요합니다.")
    created_start = dt.datetime.fromisoformat(date_config["created_start"])
    created_end = dt.datetime.fromisoformat(date_config["created_end"])
    updated_end = dt.datetime.fromisoformat(date_config["updated_end"])
    if not (created_start <= created_end <= updated_end):
        fail("place.dates의 기간 순서가 올바르지 않습니다.")


def create_settings(args: argparse.Namespace) -> DatasetSettings:
    config_path = resolve_path(args.config, Path.cwd())
    config = read_json(config_path)
    validate_config(config)
    place = config["place"]
    profile = args.profile
    profiles = place["profiles"]
    if profile not in profiles:
        fail(f"알 수 없는 profile입니다: {profile} (사용 가능: {', '.join(profiles)})")
    count = args.count if args.count is not None else int(profiles[profile])
    chunk_size = args.chunk_size if args.chunk_size is not None else int(place["chunk_size"])
    seed = args.seed if args.seed is not None else int(config["seed"])
    if count <= 0:
        fail("count는 0보다 커야 합니다.")
    if chunk_size <= 0:
        fail("chunk-size는 0보다 커야 합니다.")
    if seed < 0:
        fail("seed는 0 이상이어야 합니다.")

    content_id_config = place["content_id"]
    namespace_slots = int(content_id_config["namespace_slots"])
    namespace_capacity = int(content_id_config["namespace_capacity"])
    namespace_slot = seed % namespace_slots
    content_id_start = int(content_id_config["base"]) + (
        namespace_slot * namespace_capacity
    )
    if count > namespace_capacity:
        fail(
            f"count ({count:,}) exceeds content_id namespace capacity "
            f"({namespace_capacity:,})."
        )
    if content_id_start + count - 1 > MYSQL_SIGNED_INT_MAX:
        fail("generated content_id exceeds signed MySQL INT range.")

    repo_root = SCRIPT_DIR.parent
    configured_output = args.output_dir if args.output_dir is not None else place["output_dir"]
    output_dir = resolve_path(configured_output, repo_root)
    namespace = f"MOCK-PLACE-V2-S{seed:08X}-N{namespace_slot:02d}"
    return DatasetSettings(
        config_path=config_path,
        config=config,
        config_hash=canonical_hash(config),
        seed=seed,
        profile=profile,
        count=count,
        chunk_size=chunk_size,
        output_dir=output_dir,
        content_id_namespace=namespace,
        content_id_start=content_id_start,
    )


def weighted_entry(
    rng: random.Random,
    entries: Sequence[Mapping[str, Any]],
    weight_key: str = "weight",
) -> Mapping[str, Any]:
    total = sum(float(entry.get(weight_key, 1.0)) for entry in entries)
    cursor = rng.random() * total
    cumulative = 0.0
    for entry in entries:
        cumulative += float(entry.get(weight_key, 1.0))
        if cursor < cumulative:
            return entry
    return entries[-1]


def weighted_value(rng: random.Random, entries: Sequence[Any], key: str = "value") -> str:
    if entries and isinstance(entries[0], str):
        return str(entries[rng.randrange(len(entries))])
    return str(weighted_entry(rng, entries)[key])


def sample_coordinate(
    rng: random.Random,
    region: Mapping[str, Any],
    hotspot: Mapping[str, Any],
    global_bounds: Sequence[float],
) -> tuple[float, float]:
    min_lat, max_lat, min_lon, max_lon = map(float, region["bounds"])
    global_min_lat, global_max_lat, global_min_lon, global_max_lon = map(float, global_bounds)
    center_lat, center_lon = map(float, hotspot["center"])
    sigma_lat, sigma_lon = map(float, hotspot["sigma"])
    for _ in range(40):
        latitude = rng.gauss(center_lat, sigma_lat)
        longitude = rng.gauss(center_lon, sigma_lon)
        if (
            min_lat <= latitude <= max_lat
            and min_lon <= longitude <= max_lon
            and global_min_lat <= latitude <= global_max_lat
            and global_min_lon <= longitude <= global_max_lon
        ):
            return latitude, longitude
    return center_lat, center_lon


def build_name(
    rng: random.Random,
    settings: DatasetSettings,
    content_type: Mapping[str, Any],
    region: Mapping[str, Any],
    hotspot: Mapping[str, Any],
) -> str:
    name_config = settings.place_config["name"]
    prefix = weighted_value(rng, name_config["prefixes"])
    suffix = weighted_value(rng, content_type["suffixes"])
    rare_token = name_config["rare_token"]
    if rng.random() < float(rare_token["probability"]):
        prefix = str(rare_token["token"])
    template = weighted_value(rng, name_config["forms"], "template")
    return template.format(
        region=region.get("short_name", region["name"]),
        hotspot=hotspot["name"],
        prefix=prefix,
        suffix=suffix,
    )[:255]


def build_address(rng: random.Random, hotspot: Mapping[str, Any]) -> str:
    address_variant: Mapping[str, Any] = {}
    variants = hotspot.get("address_variants")
    if variants:
        address_variant = weighted_entry(rng, variants)
    city = address_variant.get("city", hotspot["city"])
    district = address_variant.get("district", hotspot["district"])
    neighborhood = address_variant.get("neighborhood", hotspot["neighborhood"])
    return (
        f"{city} {district} {neighborhood} "
        f"{rng.randint(1, 999)}-{rng.randint(1, 99)}"
    )


def truncate_utf8(value: str, max_bytes: int) -> str:
    while len(value.encode("utf-8")) > max_bytes:
        value = value[:-1]
    return value


def build_description(
    rng: random.Random,
    settings: DatasetSettings,
    name: str,
    address: str,
    content_type: Mapping[str, Any],
    region: Mapping[str, Any],
    hotspot: Mapping[str, Any],
) -> str | None:
    bucket = weighted_entry(
        rng,
        settings.place_config["description"]["length_buckets"],
        weight_key="probability",
    )
    if bucket["kind"] == "null":
        return None
    target = rng.randint(int(bucket["min"]), int(bucket["max"]))
    templates = (
        f"{name}은(는) {address} 인근의 {content_type['name']} 장소입니다.",
        f"{hotspot['name']} 주변의 분위기와 {region['name']}의 지역색을 함께 경험할 수 있습니다.",
        "방문 시기와 이용 목적에 따라 주변 산책로, 편의시설, 대중교통 정보를 확인하는 것을 권장합니다.",
        "가상의 성능 테스트 데이터이며 실제 운영 장소와 주소를 의미하지 않습니다.",
    )
    parts: list[str] = []
    index = 0
    while sum(len(part) for part in parts) < target + 40:
        parts.append(templates[index % len(templates)])
        index += 1
    return truncate_utf8(
        " ".join(parts)[:target],
        int(settings.place_config["description"].get("max_bytes", 60000)),
    )


def build_dates(rng: random.Random, settings: DatasetSettings) -> tuple[str, str]:
    date_config = settings.place_config["dates"]
    created_start = dt.datetime.fromisoformat(date_config["created_start"])
    created_end = dt.datetime.fromisoformat(date_config["created_end"])
    updated_end = dt.datetime.fromisoformat(date_config["updated_end"])
    created_range = int((created_end - created_start).total_seconds())
    created = created_start + dt.timedelta(seconds=rng.randint(0, created_range))
    maximum_update = int((updated_end - created).total_seconds())
    if maximum_update <= 0:
        updated = created
    else:
        configured_max = int(float(date_config.get("max_update_days", 730)) * 86400)
        updated = created + dt.timedelta(
            seconds=rng.randint(0, min(maximum_update, configured_max))
        )
    return created.strftime("%Y-%m-%d %H:%M:%S"), updated.strftime("%Y-%m-%d %H:%M:%S")


def row_seed(seed: int, index: int) -> int:
    return (seed << 32) ^ (index * 0x9E3779B1) ^ 0xD1B54A32D192ED03


def iter_place_rows(settings: DatasetSettings, start: int, end: int) -> Iterable[list[str | None]]:
    global_bounds = settings.place_config["korea_bounds"]
    for index in range(start, end):
        rng = random.Random(row_seed(settings.seed, index))
        content_type = weighted_entry(rng, settings.place_config["content_types"])
        region = weighted_entry(rng, settings.place_config["regions"])
        hotspot = weighted_entry(rng, region["hotspots"])
        latitude, longitude = sample_coordinate(rng, region, hotspot, global_bounds)
        name = build_name(rng, settings, content_type, region, hotspot)
        address = build_address(rng, hotspot)
        description = build_description(
            rng, settings, name, address, content_type, region, hotspot
        )
        src_created_at, src_updated_at = build_dates(rng, settings)
        yield [
            str(settings.content_id_start + index),
            str(content_type["id"]),
            name,
            address,
            f"{latitude:.7f}",
            f"{longitude:.7f}",
            description,
            src_created_at,
            src_updated_at,
        ]


def chunk_metadata(settings: DatasetSettings) -> list[dict[str, Any]]:
    chunks: list[dict[str, Any]] = []
    for chunk_index, start in enumerate(range(0, settings.count, settings.chunk_size)):
        end = min(start + settings.chunk_size, settings.count)
        filename = (
            f"{settings.content_id_namespace.lower()}-chunk-"
            f"{chunk_index:05d}-{start:09d}-{end - 1:09d}.csv"
        )
        chunks.append(
            {
                "index": chunk_index,
                "start": start,
                "end": end,
                "rows": end - start,
                "path": filename,
                "status": "pending",
            }
        )
    return chunks


def new_manifest(settings: DatasetSettings) -> dict[str, Any]:
    now = dt.datetime.now().isoformat(timespec="seconds")
    return {
        "manifest_version": MANIFEST_VERSION,
        "dataset": "place",
        "table": PLACE_TABLE,
        "seed": settings.seed,
        "profile": settings.profile,
        "count": settings.count,
        "chunk_size": settings.chunk_size,
        "content_id_namespace": settings.content_id_namespace,
        "content_id_start": settings.content_id_start,
        "content_id_end": settings.content_id_end,
        "config_path": str(settings.config_path),
        "config_sha256": settings.config_hash,
        "fingerprint": settings.fingerprint,
        "status": "created",
        "created_at": now,
        "updated_at": now,
        "chunks": chunk_metadata(settings),
    }


def save_manifest(path: Path, manifest: Mapping[str, Any]) -> None:
    path.parent.mkdir(parents=True, exist_ok=True)
    payload = dict(manifest)
    payload["updated_at"] = dt.datetime.now().isoformat(timespec="seconds")
    temporary_path = path.with_name(f".{path.name}.{os.getpid()}.tmp")
    with temporary_path.open("w", encoding="utf-8", newline="\n") as stream:
        json.dump(payload, stream, ensure_ascii=False, indent=2)
        stream.write("\n")
    os.replace(temporary_path, path)


def load_manifest(settings: DatasetSettings) -> dict[str, Any]:
    try:
        with settings.manifest_path.open("r", encoding="utf-8") as stream:
            manifest = json.load(stream)
    except FileNotFoundError:
        fail(f"manifest 파일이 없습니다: {settings.manifest_path}")
    except json.JSONDecodeError as exc:
        fail(f"manifest JSON이 손상되었습니다: {settings.manifest_path} ({exc})")
    if not isinstance(manifest, dict) or manifest.get("manifest_version") != MANIFEST_VERSION:
        fail("지원하지 않는 manifest입니다.")
    if manifest.get("fingerprint") != settings.fingerprint:
        fail("현재 실행 설정과 manifest가 다릅니다.")
    if manifest.get("dataset") != "place" or manifest.get("table") != PLACE_TABLE:
        fail("Place용 manifest가 아닙니다.")
    return manifest


def write_chunk(settings: DatasetSettings, chunk: Mapping[str, Any]) -> Path:
    settings.output_dir.mkdir(parents=True, exist_ok=True)
    target = settings.output_dir / str(chunk["path"])
    if target.exists():
        fail(f"이미 존재하는 CSV를 덮어쓰지 않습니다: {target}")
    temporary_path: Path | None = None
    try:
        with tempfile.NamedTemporaryFile(
            mode="w",
            encoding="utf-8",
            newline="",
            prefix=f".{target.name}.",
            suffix=".part",
            dir=settings.output_dir,
            delete=False,
        ) as stream:
            temporary_path = Path(stream.name)
            writer = csv.writer(
                stream,
                delimiter=",",
                quotechar='"',
                escapechar="\\",
                doublequote=False,
                lineterminator="\n",
                quoting=csv.QUOTE_MINIMAL,
            )
            for row in iter_place_rows(settings, int(chunk["start"]), int(chunk["end"])):
                writer.writerow(row)
        os.replace(temporary_path, target)
        return target
    except Exception:
        if temporary_path is not None:
            temporary_path.unlink(missing_ok=True)
        raise


def print_plan(settings: DatasetSettings, database: str | None = None) -> None:
    place = settings.place_config
    print("Place Mock Data plan")
    print(f"  target database : {database or '<not connected>'}")
    print(f"  target table    : {PLACE_TABLE}")
    print(f"  profile/count   : {settings.profile} / {settings.count:,}")
    print(f"  chunk size      : {settings.chunk_size:,} ({settings.chunks} chunks)")
    print(f"  seed            : {settings.seed}")
    print(
        f"  content id range: {settings.content_id_start:,}"
        f"..{settings.content_id_end:,} ({settings.content_id_namespace})"
    )
    print(f"  output directory: {settings.output_dir}")
    print("  content types   :")
    total = sum(float(item["weight"]) for item in place["content_types"])
    for item in place["content_types"]:
        expected = settings.count * float(item["weight"]) / total
        print(f"    {item['id']:>2} {item['name']}: about {expected:,.0f}")
    print("  regions         :")
    total = sum(float(item["weight"]) for item in place["regions"])
    for item in place["regions"]:
        expected = settings.count * float(item["weight"]) / total
        print(f"    {item['name']}: about {expected:,.0f}")


def load_env_file(path: Path | None) -> dict[str, str]:
    values: dict[str, str] = {}
    if path is None:
        return values
    try:
        lines = path.read_text(encoding="utf-8").splitlines()
    except FileNotFoundError:
        fail(f"env 파일을 찾을 수 없습니다: {path}")
    for line in lines:
        stripped = line.strip()
        if not stripped or stripped.startswith("#") or "=" not in stripped:
            continue
        key, value = stripped.split("=", 1)
        key = key.strip()
        value = value.strip()
        if value and value[0] in {'"', "'"} and value[-1:] == value[0]:
            value = value[1:-1]
        values[key] = value
    return values


def make_database_options(args: argparse.Namespace) -> DatabaseOptions:
    database = args.database
    if not database:
        fail("대상 DB 이름은 --database로 명시해야 합니다. 기본 DB를 사용하지 않습니다.")
    if not IDENTIFIER_RE.fullmatch(database) or len(database) > 64:
        fail("--database는 영문/숫자/_/$, 최대 64자의 MySQL identifier여야 합니다.")
    host = args.host or os.environ.get("MYSQL_HOST", "127.0.0.1")
    user = args.user or os.environ.get("MYSQL_USER") or os.environ.get("SPRING_DATASOURCE_USERNAME")
    if not user:
        fail("MySQL 사용자 이름을 --user 또는 MYSQL_USER 환경 변수로 지정해야 합니다.")
    if not IDENTIFIER_RE.fullmatch(user):
        fail("--user 형식이 올바르지 않습니다.")
    if args.port <= 0 or args.port > 65535:
        fail("--port는 1~65535 범위여야 합니다.")
    env_file = resolve_path(args.env_file, Path.cwd()) if args.env_file else None
    mysql_bin = args.mysql_bin or shutil.which("mysql") or "mysql"
    return DatabaseOptions(
        database=database,
        host=host,
        port=args.port,
        user=user,
        password_env=args.password_env,
        env_file=env_file,
        mysql_bin=mysql_bin,
    )


def mysql_environment(options: DatabaseOptions) -> dict[str, str]:
    environment = os.environ.copy()
    file_values = load_env_file(options.env_file)
    for key, value in file_values.items():
        environment.setdefault(key, value)
    password = environment.get(options.password_env)
    if not password:
        for fallback in ("MYSQL_PWD", "MYSQL_PASSWORD", "SPRING_DATASOURCE_PASSWORD"):
            if environment.get(fallback):
                password = environment[fallback]
                break
    if not password:
        if sys.stdin.isatty():
            import getpass

            password = getpass.getpass("MySQL password: ")
        else:
            fail(
                f"MySQL 비밀번호를 {options.password_env} 환경 변수 또는 --env-file로 지정해야 합니다."
            )
    environment["MYSQL_PWD"] = password
    return environment


def run_mysql(options: DatabaseOptions, sql: str) -> str:
    command = [
        options.mysql_bin,
        "--protocol=tcp",
        "--host",
        options.host,
        "--port",
        str(options.port),
        "--user",
        options.user,
        "--database",
        options.database,
        "--default-character-set=utf8mb4",
        "--local-infile=1",
        "--batch",
        "--raw",
        "--skip-column-names",
        "--execute",
        sql,
    ]
    try:
        result = subprocess.run(
            command,
            env=mysql_environment(options),
            capture_output=True,
            text=True,
            encoding="utf-8",
            errors="replace",
            check=False,
        )
    except FileNotFoundError:
        fail(
            f"mysql CLI를 찾을 수 없습니다: {options.mysql_bin}. "
            "--mysql-bin으로 실행 파일 경로를 지정하세요."
        )
    if result.returncode != 0:
        fail(
            f"MySQL 명령이 실패했습니다 (exit={result.returncode}): "
            f"{result.stderr.strip()}"
        )
    return result.stdout.strip()


def sql_literal(value: str) -> str:
    return "'" + value.replace("\\", "\\\\").replace("'", "\\'").replace("\n", "\\n") + "'"


def query_rows(options: DatabaseOptions, sql: str) -> list[list[str]]:
    output = run_mysql(options, sql)
    if not output:
        return []
    return [line.split("\t") for line in output.splitlines()]


def scalar(options: DatabaseOptions, sql: str) -> str:
    rows = query_rows(options, sql)
    if not rows or not rows[0]:
        fail("MySQL 조회 결과가 비어 있습니다.")
    return rows[0][0]


def count_for_namespace(
    options: DatabaseOptions, settings: DatasetSettings
) -> int:
    return int(
        scalar(
            options,
            "SELECT COUNT(*) FROM place WHERE content_id BETWEEN "
            + str(settings.content_id_start)
            + " AND "
            + str(settings.content_id_end),
        )
    )


def count_for_chunk(
    options: DatabaseOptions, settings: DatasetSettings, chunk: Mapping[str, Any]
) -> int:
    first_id = settings.content_id_start + int(chunk["start"])
    last_id = settings.content_id_start + int(chunk["end"]) - 1
    return int(
        scalar(
            options,
            "SELECT COUNT(*) FROM place WHERE content_id >= "
            + str(first_id)
            + " AND content_id <= "
            + str(last_id),
        )
    )


def preflight(
    options: DatabaseOptions,
    settings: DatasetSettings,
    allow_existing_namespace: bool,
    require_local_infile: bool,
) -> int:
    print(
        f"[preflight] database={options.database}, table={PLACE_TABLE}, "
        f"expected={settings.count:,}"
    )
    database_rows = query_rows(
        options,
        "SELECT DATABASE(), @@local_infile, @@character_set_database, "
        "@@collation_database",
    )
    if not database_rows or database_rows[0][0] != options.database:
        fail("지정한 대상 DB로 연결되지 않았습니다.")
    local_infile = database_rows[0][1]
    if require_local_infile and local_infile != "1":
        fail(
            "MySQL 서버의 local_infile이 OFF입니다. "
            "Docker를 사용하면 mysql command에 --local-infile=1을 적용하고 서버를 재시작하세요."
        )

    table_rows = query_rows(
        options,
        "SELECT TABLE_TYPE FROM information_schema.TABLES "
        "WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'place'",
    )
    if not table_rows or table_rows[0][0] != "BASE TABLE":
        fail("대상 DB에 BASE TABLE place가 없습니다.")

    columns = query_rows(options, "SHOW COLUMNS FROM place")
    column_map = {row[0]: row for row in columns if row}
    for column in PLACE_LOAD_COLUMNS:
        if column not in column_map:
            fail(f"place 테이블에 필요한 컬럼이 없습니다: {column}")
    if column_map.get("content_id", [None, None, None, None])[3] != "UNI":
        fail("place.content_id에 UNIQUE 제약이 없습니다.")
    expected_types = {
        "content_id": "int",
        "content_type_id": "int",
        "name": "varchar(255)",
        "address": "varchar(255)",
        "latitude": "double",
        "longitude": "double",
        "description": "text",
        "src_created_at": "datetime",
        "src_updated_at": "datetime",
    }
    for column, expected_type in expected_types.items():
        actual_type = column_map[column][1].lower()
        if actual_type != expected_type:
            fail(
                f"place.{column} 타입이 init.sql 기준과 다릅니다: "
                f"{actual_type} != {expected_type}"
            )
    for required_not_null in (
        "content_id",
        "content_type_id",
        "name",
        "address",
        "latitude",
        "longitude",
        "src_created_at",
        "src_updated_at",
    ):
        if column_map[required_not_null][2] != "NO":
            fail(f"place.{required_not_null}가 NOT NULL이 아닙니다.")
    if column_map["description"][2] != "YES":
        fail("place.description이 NULL 허용이 아닙니다.")

    foreign_key_rows = query_rows(
        options,
        "SELECT REFERENCED_TABLE_NAME, REFERENCED_COLUMN_NAME "
        "FROM information_schema.KEY_COLUMN_USAGE "
        "WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'place' "
        "AND COLUMN_NAME = 'content_type_id' "
        "AND REFERENCED_TABLE_NAME = 'content_type' "
        "AND REFERENCED_COLUMN_NAME = 'content_type_id'",
    )
    if not foreign_key_rows:
        fail("place.content_type_id -> content_type.content_type_id FK를 확인할 수 없습니다.")

    content_type_ids = [str(int(item["id"])) for item in settings.place_config["content_types"]]
    actual_content_types = query_rows(
        options,
        "SELECT content_type_id, name FROM content_type WHERE content_type_id IN ("
        + ", ".join(content_type_ids)
        + ") ORDER BY content_type_id",
    )
    expected_content_types = {
        str(int(item["id"])): str(item["name"])
        for item in settings.place_config["content_types"]
    }
    actual_map = {row[0]: row[1] for row in actual_content_types if len(row) >= 2}
    missing = sorted(set(expected_content_types) - set(actual_map))
    mismatched = sorted(
        key
        for key, expected_name in expected_content_types.items()
        if key in actual_map and actual_map[key] != expected_name
    )
    if missing:
        fail(
            "대상 DB에 필요한 content_type 기준 행이 없습니다: "
            + ", ".join(missing)
            + ". init.sql의 정적 기준 데이터를 먼저 적용하세요."
        )
    if mismatched:
        fail("content_type_id의 이름이 init.sql 기준과 다릅니다: " + ", ".join(mismatched))

    existing = count_for_namespace(options, settings)
    print(
        f"[preflight] existing numeric namespace rows={existing:,}, "
        f"local_infile={local_infile}"
    )
    if existing and not allow_existing_namespace:
        fail(
            f"동일 Mock namespace의 기존 Place가 {existing:,}건 있습니다. "
            "중복 적재를 막기 위해 중단합니다. 재개가 맞으면 --resume을 사용하세요."
        )
    return existing


def generate_command(settings: DatasetSettings, args: argparse.Namespace) -> int:
    print_plan(settings)
    if args.dry_run:
        return 0
    settings.output_dir.mkdir(parents=True, exist_ok=True)
    if settings.manifest_path.exists():
        if not args.resume:
            fail(
                f"manifest가 이미 존재합니다: {settings.manifest_path}. "
                "재개가 맞으면 --resume을 사용하세요."
            )
        manifest = load_manifest(settings)
    else:
        manifest = new_manifest(settings)
        save_manifest(settings.manifest_path, manifest)

    manifest["status"] = "generating"
    save_manifest(settings.manifest_path, manifest)
    try:
        for chunk in manifest["chunks"]:
            if chunk["status"] == "loaded":
                continue
            target = settings.output_dir / str(chunk["path"])
            if target.exists():
                if chunk["status"] == "generated":
                    print(f"[generate] chunk={chunk['index']} already generated; skipped")
                    continue
                fail(
                    f"manifest상 미완료 chunk의 CSV가 이미 존재합니다: {target}. "
                    "부분 파일인지 확인 후 수동으로 정리하세요."
                )
            started = time.monotonic()
            write_chunk(settings, chunk)
            chunk["status"] = "generated"
            chunk["bytes"] = target.stat().st_size
            chunk["generated_seconds"] = round(time.monotonic() - started, 3)
            save_manifest(settings.manifest_path, manifest)
            print(f"[generate] chunk={chunk['index']} rows={chunk['rows']:,} file={target}")
        manifest["status"] = "generated"
        save_manifest(settings.manifest_path, manifest)
    except Exception as exc:
        manifest["status"] = "failed"
        manifest["error"] = str(exc)
        save_manifest(settings.manifest_path, manifest)
        raise
    return 0


def load_data_file(options: DatabaseOptions, path: Path) -> None:
    mysql_path = path.resolve().as_posix()
    columns = ", ".join(PLACE_LOAD_COLUMNS)
    sql = (
        "LOAD DATA LOCAL INFILE "
        + sql_literal(mysql_path)
        + " INTO TABLE place\n"
        "CHARACTER SET utf8mb4\n"
        "FIELDS TERMINATED BY ',' OPTIONALLY ENCLOSED BY '\"' ESCAPED BY '\\\\'\n"
        "LINES TERMINATED BY '\\n'\n"
        f"({columns.replace('description', '@description')})\n"
        "SET description = NULLIF(@description, '')"
    )
    run_mysql(options, sql)


def create_or_load_manifest(settings: DatasetSettings, resume: bool) -> dict[str, Any]:
    settings.output_dir.mkdir(parents=True, exist_ok=True)
    if settings.manifest_path.exists():
        if not resume:
            fail(
                f"manifest가 이미 존재합니다: {settings.manifest_path}. "
                "재개가 맞으면 --resume을 사용하세요."
            )
        return load_manifest(settings)
    if resume:
        fail("--resume에는 기존 manifest가 필요합니다.")
    manifest = new_manifest(settings)
    save_manifest(settings.manifest_path, manifest)
    return manifest


def load_chunks(
    options: DatabaseOptions,
    settings: DatasetSettings,
    manifest: dict[str, Any],
    resume: bool,
    keep_files: bool,
    generate_on_demand: bool,
) -> int:
    preflight(
        options,
        settings,
        allow_existing_namespace=resume,
        require_local_infile=True,
    )
    manifest["status"] = "loading"
    save_manifest(settings.manifest_path, manifest)
    try:
        for chunk in manifest["chunks"]:
            expected_rows = int(chunk["rows"])
            existing_rows = count_for_chunk(options, settings, chunk)
            if existing_rows == expected_rows:
                chunk["status"] = "loaded"
                chunk["loaded_by"] = "existing-check"
                save_manifest(settings.manifest_path, manifest)
                print(f"[load] chunk={chunk['index']} already complete; skipped")
                if not keep_files:
                    (settings.output_dir / str(chunk["path"])).unlink(missing_ok=True)
                continue
            if existing_rows > 0:
                fail(
                    f"chunk={chunk['index']}가 부분 적재 상태입니다: "
                    f"{existing_rows:,}/{expected_rows:,}. 자동 재적재하지 않습니다."
                )

            path = settings.output_dir / str(chunk["path"])
            if not path.exists():
                if not generate_on_demand:
                    fail(f"chunk CSV가 없습니다: {path}. generate를 먼저 실행하세요.")
                write_chunk(settings, chunk)
                chunk["status"] = "generated"
                chunk["bytes"] = path.stat().st_size
                save_manifest(settings.manifest_path, manifest)

            started = time.monotonic()
            load_data_file(options, path)
            loaded_rows = count_for_chunk(options, settings, chunk)
            if loaded_rows != expected_rows:
                fail(
                    f"chunk={chunk['index']} 적재 후 row 수가 다릅니다: "
                    f"{loaded_rows:,}/{expected_rows:,}. CSV를 보존합니다."
                )
            chunk["status"] = "loaded"
            chunk["loaded_seconds"] = round(time.monotonic() - started, 3)
            save_manifest(settings.manifest_path, manifest)
            print(
                f"[load] chunk={chunk['index']} rows={expected_rows:,} "
                f"elapsed={chunk['loaded_seconds']}s"
            )
            if not keep_files:
                path.unlink(missing_ok=True)

        total = count_for_namespace(options, settings)
        if total != settings.count:
            fail(f"전체 namespace row 수가 다릅니다: {total:,}/{settings.count:,}")
        manifest["status"] = "complete"
        manifest["loaded_rows"] = total
        save_manifest(settings.manifest_path, manifest)
        print(f"[load] complete rows={total:,}")
    except Exception as exc:
        manifest["status"] = "failed"
        manifest["error"] = str(exc)
        save_manifest(settings.manifest_path, manifest)
        raise
    return 0


def print_verification(options: DatabaseOptions, settings: DatasetSettings) -> None:
    preflight(
        options,
        settings,
        allow_existing_namespace=True,
        require_local_infile=False,
    )
    scope = (
        "content_id BETWEEN "
        + str(settings.content_id_start)
        + " AND "
        + str(settings.content_id_end)
    )
    total = int(scalar(options, f"SELECT COUNT(*) FROM place WHERE {scope}"))
    duplicate_count = int(
        scalar(
            options,
            f"SELECT COUNT(*) - COUNT(DISTINCT content_id) FROM place WHERE {scope}",
        )
    )
    type_rows = query_rows(
        options,
        f"SELECT content_type_id, COUNT(*) FROM place WHERE {scope} "
        "GROUP BY content_type_id ORDER BY content_type_id",
    )
    description_counts = query_rows(
        options,
        f"SELECT "
        f"SUM(CASE WHEN description IS NULL THEN 1 ELSE 0 END), "
        f"SUM(CASE WHEN description IS NOT NULL AND CHAR_LENGTH(description) < 30 THEN 1 ELSE 0 END), "
        f"SUM(CASE WHEN description IS NOT NULL AND CHAR_LENGTH(description) BETWEEN 30 AND 100 THEN 1 ELSE 0 END), "
        f"SUM(CASE WHEN description IS NOT NULL AND CHAR_LENGTH(description) BETWEEN 101 AND 300 THEN 1 ELSE 0 END), "
        f"SUM(CASE WHEN description IS NOT NULL AND CHAR_LENGTH(description) BETWEEN 301 AND 700 THEN 1 ELSE 0 END), "
        f"SUM(CASE WHEN description IS NOT NULL AND CHAR_LENGTH(description) >= 701 THEN 1 ELSE 0 END) "
        f"FROM place WHERE {scope}",
    )[0]
    (
        null_description,
        short_description,
        medium_description,
        long_description,
        very_long_description,
        extra_long_description,
    ) = (
        int(value) for value in description_counts
    )
    coordinate_row = query_rows(
        options,
        f"SELECT MIN(latitude), MAX(latitude), MIN(longitude), MAX(longitude), "
        f"SUM(CASE WHEN latitude < {float(settings.place_config['korea_bounds'][0])} "
        f"OR latitude > {float(settings.place_config['korea_bounds'][1])} "
        f"OR longitude < {float(settings.place_config['korea_bounds'][2])} "
        f"OR longitude > {float(settings.place_config['korea_bounds'][3])} THEN 1 ELSE 0 END) "
        f"FROM place WHERE {scope}",
    )[0]
    min_lat, max_lat, min_lon, max_lon, outside_bounds = coordinate_row
    date_row = query_rows(
        options,
        f"SELECT MIN(src_created_at), MAX(src_updated_at), "
        f"SUM(CASE WHEN src_created_at > src_updated_at THEN 1 ELSE 0 END) "
        f"FROM place WHERE {scope}",
    )[0]
    min_created, max_updated, invalid_dates = date_row

    print("Place Mock Data verification")
    print(f"  namespace rows       : {total:,} (expected {settings.count:,})")
    print(f"  content_id duplicates: {duplicate_count:,}")
    print("  content_type counts  :")
    for row in type_rows:
        print(f"    {row[0]}: {int(row[1]):,}")

    case_parts: list[str] = []
    for region in settings.place_config["regions"]:
        predicates = " OR ".join(
            "address LIKE " + sql_literal(str(prefix) + "%")
            for prefix in region["address_prefixes"]
        )
        case_parts.append(
            f"WHEN {predicates} THEN {sql_literal(str(region['name']))}"
        )
    region_rows = query_rows(
        options,
        f"SELECT CASE {' '.join(case_parts)} ELSE '기타' END, COUNT(*) "
        f"FROM place WHERE {scope} GROUP BY 1 ORDER BY 1",
    )
    print("  address region counts:")
    for row in region_rows:
        print(f"    {row[0]}: {int(row[1]):,}")

    print("  description length  :")
    for label, value in (
        ("NULL", null_description),
        ("< 30", short_description),
        ("30-100", medium_description),
        ("101-300", long_description),
        ("301-700", very_long_description),
        (">= 701", extra_long_description),
    ):
        ratio = value / total * 100 if total else 0.0
        print(f"    {label:>7}: {value:,} ({ratio:.2f}%)")
    print(
        f"  coordinate range     : lat {min_lat}..{max_lat}, "
        f"lon {min_lon}..{max_lon}, outside={outside_bounds}"
    )
    print(f"  source date range    : {min_created} .. {max_updated}, invalid order={invalid_dates}")
    print("  name token counts    :")
    for token in settings.place_config["name"].get("verification_tokens", []):
        token_value = str(token["token"])
        token_count = int(
            scalar(
                options,
                f"SELECT SUM(name LIKE {sql_literal('%' + token_value + '%')}) "
                f"FROM place WHERE {scope}",
            )
        )
        print(f"    {token_value}: {token_count:,}")


def run_command(settings: DatasetSettings, args: argparse.Namespace) -> int:
    options = make_database_options(args)
    print_plan(settings, options.database)
    if args.dry_run:
        preflight(
            options,
            settings,
            allow_existing_namespace=bool(args.resume),
            require_local_infile=True,
        )
        print("[dry-run] schema/FK/content_type/local_infile 검증만 수행했습니다. 적재하지 않았습니다.")
        return 0
    manifest = create_or_load_manifest(settings, args.resume)
    return load_chunks(
        options,
        settings,
        manifest,
        resume=args.resume,
        keep_files=args.keep_files,
        generate_on_demand=True,
    )


def load_command(settings: DatasetSettings, args: argparse.Namespace) -> int:
    options = make_database_options(args)
    print_plan(settings, options.database)
    manifest = load_manifest(settings)
    if args.dry_run:
        preflight(
            options,
            settings,
            allow_existing_namespace=True,
            require_local_infile=True,
        )
        print("[dry-run] 기존 CSV/manifest와 DB schema 검증만 수행했습니다. 적재하지 않았습니다.")
        return 0
    return load_chunks(
        options,
        settings,
        manifest,
        resume=True,
        keep_files=args.keep_files,
        generate_on_demand=False,
    )


def verify_command(settings: DatasetSettings, args: argparse.Namespace) -> int:
    options = make_database_options(args)
    print_plan(settings, options.database)
    print_verification(options, settings)
    return 0


def build_parser() -> argparse.ArgumentParser:
    parser = argparse.ArgumentParser(description=__doc__)
    parser.add_argument(
        "--config",
        default=str(DEFAULT_CONFIG_PATH),
        help="JSON 설정 파일 경로 (기본: mock_data/place_mock_config.json)",
    )
    subparsers = parser.add_subparsers(dest="command", required=True)

    def add_dataset_args(command_parser: argparse.ArgumentParser) -> None:
        command_parser.add_argument(
            "--profile",
            default="real",
        )
        command_parser.add_argument("--count", type=int, help="profile 대신 사용할 row 수")
        command_parser.add_argument("--chunk-size", type=int, help="chunk 크기 override")
        command_parser.add_argument("--seed", type=int, help="설정 seed override")
        command_parser.add_argument("--output-dir", help="CSV/manifest 출력 디렉터리")

    def add_db_args(command_parser: argparse.ArgumentParser) -> None:
        command_parser.add_argument("--database", help="필수: 적재 대상 MySQL DB 이름")
        command_parser.add_argument(
            "--host",
            help="MySQL host (기본: MYSQL_HOST 또는 127.0.0.1)",
        )
        command_parser.add_argument("--port", type=int, default=3306)
        command_parser.add_argument(
            "--user",
            help="MySQL user (기본: MYSQL_USER 또는 SPRING_DATASOURCE_USERNAME)",
        )
        command_parser.add_argument(
            "--password-env",
            default="MYSQL_PWD",
            help="비밀번호 환경 변수 이름",
        )
        command_parser.add_argument("--env-file", help="비밀번호를 읽을 env 파일 경로")
        command_parser.add_argument("--mysql-bin", help="mysql CLI 실행 파일 경로")

    generate_parser = subparsers.add_parser("generate", help="CSV chunk만 생성")
    add_dataset_args(generate_parser)
    generate_parser.add_argument("--resume", action="store_true")
    generate_parser.add_argument("--dry-run", action="store_true")

    run_parser = subparsers.add_parser(
        "run",
        help="CSV 생성과 MySQL bulk load를 chunk별로 수행",
    )
    add_dataset_args(run_parser)
    add_db_args(run_parser)
    run_parser.add_argument("--resume", action="store_true")
    run_parser.add_argument("--keep-files", action="store_true")
    run_parser.add_argument("--dry-run", action="store_true")

    load_parser = subparsers.add_parser(
        "load",
        help="generate가 만든 CSV manifest를 MySQL에 적재",
    )
    add_dataset_args(load_parser)
    add_db_args(load_parser)
    load_parser.add_argument("--keep-files", action="store_true")
    load_parser.add_argument("--dry-run", action="store_true")

    verify_parser = subparsers.add_parser(
        "verify",
        help="생성 namespace의 분포와 제약조건 검증",
    )
    add_dataset_args(verify_parser)
    add_db_args(verify_parser)

    return parser


def dispatch(args: argparse.Namespace) -> int:
    settings = create_settings(args)
    if args.command == "generate":
        return generate_command(settings, args)
    if args.command == "run":
        return run_command(settings, args)
    if args.command == "load":
        return load_command(settings, args)
    if args.command == "verify":
        return verify_command(settings, args)
    fail(f"지원하지 않는 command입니다: {args.command}")
    return 2


def main(argv: Sequence[str] | None = None) -> int:
    parser = build_parser()
    args = parser.parse_args(argv)
    try:
        return dispatch(args)
    except GeneratorError as exc:
        print(f"ERROR: {exc}", file=sys.stderr)
        return 2
    except KeyboardInterrupt:
        print(
            "중단되었습니다. manifest와 미완료 CSV를 확인한 뒤 --resume으로 재개하세요.",
            file=sys.stderr,
        )
        return 130


if __name__ == "__main__":
    raise SystemExit(main())
