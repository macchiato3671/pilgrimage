from copy import deepcopy
from datetime import datetime, timezone
from typing import Any

from fastapi import Depends, Header, HTTPException, status

from data import state


Member = dict[str, Any]


def now_iso() -> str:
    return datetime.now(timezone.utc).replace(microsecond=0).isoformat().replace("+00:00", "Z")


def error(status_code: int, error_code: str, message: str) -> HTTPException:
    return HTTPException(status_code=status_code, detail={"errorCode": error_code, "message": message})


def pick(item: dict[str, Any], keys: tuple[str, ...]) -> dict[str, Any]:
    return {key: deepcopy(item[key]) for key in keys}


def member_payload(member: Member) -> dict[str, Any]:
    return pick(member, ("memberId", "email", "nickname", "role", "status", "createdAt"))


def scene_payload(scene: dict[str, Any]) -> dict[str, Any]:
    return pick(scene, ("sceneId", "dramaId", "name", "description", "address", "latitude", "longitude", "imgUrl"))


def place_payload(place: dict[str, Any]) -> dict[str, Any]:
    return pick(place, ("placeId", "name", "description", "address", "latitude", "longitude", "imgUrl", "contentId", "contentTypeId", "contentTypeName"))


def drama_list_payload(drama: dict[str, Any]) -> dict[str, Any]:
    return {"id": drama["dramaId"], **pick(drama, ("title", "genres", "posterUrl", "releasedAt"))}


def plan_payload(plan: dict[str, Any]) -> dict[str, Any]:
    return pick(plan, ("planId", "memberId", "title", "beginDate", "endDate", "details", "createdAt", "updatedAt"))


def current_member(authorization: str | None = Header(default=None, alias="Authorization")) -> Member:
    if not authorization or not authorization.startswith("Bearer "):
        raise error(status.HTTP_401_UNAUTHORIZED, "UNAUTHORIZED", "Authorization header is required.")
    token = authorization.removeprefix("Bearer ").strip()
    member_id = state["tokens"].get(token)
    member = state["members"].get(member_id)
    if not member or member["status"] != "ACTIVE":
        raise error(status.HTTP_401_UNAUTHORIZED, "INVALID_OR_EXPIRED_TOKEN", "Invalid or expired token.")
    return member


def admin_member(member: Member = Depends(current_member)) -> Member:
    if member["role"] != "ADMIN":
        raise error(status.HTTP_403_FORBIDDEN, "ADMIN_AUTHORITY_REQUIRED", "Admin authority is required.")
    return member


def get_scene(scene_id: int) -> dict[str, Any]:
    scene = state["scenes"].get(scene_id)
    if not scene:
        raise error(status.HTTP_404_NOT_FOUND, "SCENE_NOT_FOUND", "Scene not found.")
    return scene


def wishlist_payload(item: dict[str, Any]) -> dict[str, Any]:
    return {**pick(item, ("wishlistId", "createdAt")), "scene": scene_payload(get_scene(item["sceneId"]))}


def find_wishlist(member_id: int, scene_id: int) -> tuple[int, dict[str, Any]] | None:
    return next(
        ((item_id, item) for item_id, item in state["wishlist"].items() if item["memberId"] == member_id and item["sceneId"] == scene_id),
        None,
    )


def wishlist_with_scene(item: dict[str, Any], scene: dict[str, Any]) -> dict[str, Any]:
    return {**deepcopy(item), "scene": scene_payload(scene)}


def get_plan_for_member(plan_id: int, member: Member) -> dict[str, Any]:
    plan = state["plans"].get(plan_id)
    if not plan:
        raise error(status.HTTP_404_NOT_FOUND, "TRAVEL_PLAN_NOT_FOUND", "Travel plan not found.")
    if member["role"] != "ADMIN" and plan["memberId"] != member["memberId"]:
        raise error(status.HTTP_403_FORBIDDEN, "TRAVEL_PLAN_ACCESS_DENIED", "Travel plan access is denied.")
    return plan


def next_id(counter: str) -> int:
    value = state[counter]
    state[counter] += 1
    return value


def build_plan_details(details: list[Any]) -> list[dict[str, Any]]:
    return [{"detailId": next_id("nextDetailId"), **detail.model_dump()} for detail in details]


def plan_values(payload: Any) -> dict[str, Any]:
    return {
        "title": payload.title,
        "beginDate": payload.beginDate,
        "endDate": payload.endDate,
        "details": build_plan_details(payload.details),
    }


def update_present(target: dict[str, Any], source: Any, fields: tuple[str, ...]) -> None:
    target.update({field: value for field in fields if (value := getattr(source, field)) is not None})
