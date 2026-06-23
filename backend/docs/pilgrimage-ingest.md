# Pilgrimage Ingest Batch

## Architecture and Step Flow

`pilgrimageIngestJob` is one restartable Spring Batch job:

1. `discoverPostsStep`: sequentially crawls the Tistory category pages and upserts `CrawlPost`.
2. `parsePostsStep`: chunk size 1, downloads each post, extracts the drama query and scene blocks, and upserts `CrawlScene`.
3. `syncDramasStep`: matches TMDB TV series only, upserts `Drama`, `Genre`, `DramaGenre`, and creates TMDB image tasks.
4. `syncScenesStep`: normalizes scene addresses, geocodes through Kakao, saves only domestic scenes, and creates blog image tasks.
5. `uploadImagesStep`: chunk size 1, downloads images, converts to WebP, uploads to deterministic S3 keys, and upserts `DramaImg`/`SceneImg`.
6. `summarizeIngestStep`: logs aggregate counts from staging and final tables.

External crawling/API/S3/image work is not wrapped in one large DB transaction. Staging tables record explicit statuses so reruns can continue safely.

## Required Environment Variables

```bash
TMDB_READ_TOKEN=...
KAKAO_REST_API_KEY=...
AWS_REGION=ap-northeast-2
S3_BUCKET=your-bucket
S3_PUBLIC_BASE_URL= # optional
CWEBP_PATH=cwebp
GIF2WEBP_PATH=gif2webp
IMAGEMAGICK_PATH=magick
IMAGE_TEMP_DIR=/tmp/pilgrimage-ingest
```

AWS credentials are read by the AWS SDK default credential provider chain. Do not put access keys in project files.

## TMDB, Kakao, and AWS

TMDB uses the TV API only and authenticates with `Authorization: Bearer ${TMDB_READ_TOKEN}`. Kakao Local uses `Authorization: KakaoAK ${KAKAO_REST_API_KEY}`. S3 writes WebP objects with `Content-Type: image/webp` and immutable cache headers.

Minimum S3 IAM permissions for the configured prefix:

```text
s3:PutObject
s3:GetObject
```

Add narrowly scoped `s3:ListBucket` only if object listing is added later.

## DB Migrations

Flyway is enabled. Existing non-Flyway databases are baselined at version 1, then `V2__pilgrimage_batch_ingest.sql` runs.

```bash
env JAVA_HOME=/opt/jdk/temurin-21/jdk-21.0.11+10 \
  PATH=/opt/jdk/temurin-21/jdk-21.0.11+10/bin:$PATH \
  bash ./mvnw test
```

The migration creates/upgrades the domain tables, staging tables, geocode cache, override table, image task table, and Spring Batch metadata tables.

## Image Binaries

Install:

```bash
sudo apt-get install webp imagemagick
```

`webp` provides `cwebp` and `gif2webp`. SVG inputs are rasterized with ImageMagick before WebP conversion.

## Local Execution

Batch auto-run is disabled for normal web startup. Enable it explicitly:

```bash
java -jar target/pilgrimage-0.0.1-SNAPSHOT.jar \
  --spring.batch.job.enabled=true \
  --spring.batch.job.name=pilgrimageIngestJob \
  ingestKey=initial-v1 \
  fullScan=true \
  retryFailed=true \
  prune=false
```

Use the same `ingestKey` to restart a failed execution. Use a new `ingestKey` for a new completed ingest run.

## Docker Execution

```bash
bash ./mvnw -DskipTests package
docker compose -f docker-compose.pilgrimage.yml up mysql
docker compose -f docker-compose.pilgrimage.yml --profile batch run --rm pilgrimage-ingest
```

The container runs as a non-root user and writes temporary image files under `/tmp/pilgrimage-ingest`.

## Restart Procedure

If the job fails, rerun with the same identifying parameters:

```bash
java -jar target/pilgrimage-0.0.1-SNAPSHOT.jar \
  --spring.batch.job.enabled=true \
  --spring.batch.job.name=pilgrimageIngestJob \
  ingestKey=initial-v1 \
  fullScan=true \
  retryFailed=true \
  prune=false
```

Spring Batch resumes incomplete steps for the same failed `JobInstance`. Completed steps are not rerun by default. Staging statuses also make a new run idempotent if a new `ingestKey` is used.

## DramaMatchOverride

Use overrides when TMDB search is ambiguous or a title is known to need a specific TV ID:

```sql
INSERT INTO DramaMatchOverride (normalized_title, tmdb_id, memo)
VALUES ('선재업고튀어', 218589, 'Force correct TMDB TV match')
ON DUPLICATE KEY UPDATE tmdb_id = VALUES(tmdb_id), memo = VALUES(memo);
```

`normalized_title` must match `DramaTitleNormalizer.normalizeTitle(...)`.

## Result Checks

```sql
SELECT status, COUNT(*) FROM CrawlPost GROUP BY status;
SELECT status, COUNT(*) FROM CrawlScene GROUP BY status;
SELECT status, COUNT(*) FROM ImageImportTask GROUP BY status;
SELECT COUNT(*) FROM Drama;
SELECT COUNT(*) FROM Genre;
SELECT COUNT(*) FROM DramaGenre;
SELECT COUNT(*) FROM Scene;
SELECT COUNT(*) FROM DramaImg;
SELECT COUNT(*) FROM SceneImg;
SELECT cp.post_title, cp.error_code, cp.error_message FROM CrawlPost cp WHERE cp.status <> 'TMDB_MATCHED';
SELECT raw_name, raw_address, status, error_code FROM CrawlScene WHERE status <> 'SCENE_SAVED';
```

## Key Statuses

`DISCOVERED`: post URL found.
`PARSED`: post or scene parsed into staging.
`TMDB_MATCHED`: TV series confirmed and domain drama rows synced.
`TMDB_NOT_FOUND` / `TMDB_AMBIGUOUS`: no safe TMDB match; final drama sync is skipped.
`SCENE_SAVED`: domestic geocoded scene saved.
`NON_DOMESTIC`: Kakao result is outside the accepted South Korean regions.
`GEOCODE_NOT_FOUND` / `GEOCODE_AMBIGUOUS`: scene remains in staging and is not inserted into `Scene`.
`IMAGE_PENDING`: image task created.
`COMPLETED`: WebP uploaded and final image row saved.
`FAILED`: retryable or operational failure with `error_code` and `error_message`.

## Foreign Location Policy

Only Kakao results whose `region_1depth_name` is one of the accepted South Korean administrative regions are saved to `Scene`. Foreign, unclear, not-found, ambiguous, or coordinate-less locations remain in `CrawlScene` with a status and are not assigned fake coordinates. Blog images for those scenes are not uploaded.

## S3 Object Key Rules

Object keys are based on the final converted WebP SHA-256:

```text
{prefix}/dramas/{dramaId}/posters/{sha256}.webp
{prefix}/dramas/{dramaId}/backdrops/{sha256}.webp
{prefix}/dramas/{dramaId}/logos/{sha256}.webp
{prefix}/dramas/{dramaId}/scenes/{sceneId}/{sha256}.webp
```

`object_key` is canonical. `url` is either `{S3_PUBLIC_BASE_URL}/{object_key}` or `s3://{bucket}/{object_key}`. Presigned URLs are not stored.
