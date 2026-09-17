# Performance Dataset v1

`mysql-perf`에 반복 적재할 고정 Dataset의 규모·분포·검증 계약이다.
생성기는 현재 브랜치의 [generate_place_mock.py](../../mock_data/generate_place_mock.py)를 사용한다(원본 커밋 `60c9db2`).
이름·주소 조합과 분포는 [JSON 설정](../../mock_data/place_mock_config.json), 상세 사용법은 [Generator README](../../mock_data/README.md)를 참조한다.
하네스의 접속 대상과 실행 조건은 아래 로컬 실행 계약을 따른다.

## 식별 정보와 규모

| 항목 | 값 |
|---|---|
| Dataset ID | `perf-dataset-v1` |
| 생성 설정 | seed `3671`, profile `real`, chunk `500000` |
| `place` | 6,000,000건, Generator가 생성 |
| `content_type` | 기준 행 8개, Generator가 생성하지 않으므로 사전 적재 필요 |
| 관계 | `place.content_type_id` → `content_type.content_type_id` |
| 무결성 | `content_id` UNIQUE, FK / NOT NULL 조건 준수 |

## 검색 성능에 영향을 주는 분포

검색은 `name`·`address`·`description` 부분 검색, 유형 필터, 좌표 범위와 `place_id` 정렬·페이지 처리를 사용한다.
아래 비율은 Generator의 기대값이며 실제 Count는 적재 후 기록한다.

### 콘텐츠 유형

| content_type_id | 기대 비율 |
|---:|---:|
| 12 | 24% |
| 14 | 15% |
| 15 | 7% |
| 25 | 5% |
| 28 | 12% |
| 32 | 13% |
| 38 | 10% |
| 39 | 14% |

### 지역과 좌표

좌표는 34개 Hotspot 중심 주변의 Gaussian 분포로 군집된다.
허용 범위는 위도 `33.0~38.6`, 경도 `126.0~129.7`이다.

| 지역권 | 기대 비율 |
|---|---:|
| 수도권 | 52% |
| 부산/울산 | 12% |
| 대구/경북 | 9% |
| 대전/충청 | 10% |
| 광주/전라 | 8% |
| 강원 | 5% |
| 제주 | 4% |

### 검색 문자열

- 이름은 지역·Hotspot·접두사·유형별 접미사를 조합한다. 조합 비중과 후보 목록은 JSON 설정에 둔다.
- 주소는 지역·Hotspot별 후보를 조합한 합성 데이터다.
- 일반 검색 토큰은 `공원`, 복합 검색 토큰은 `문화`다.
- 희귀 이름 토큰 `별빛누리`의 생성 확률은 0.02%이며 실제 매칭 Count를 확인한다.
- `description`은 장소명·주소·유형·지역 관련 문장으로 구성된다.

| description 값 | 기대 비율 |
|---|---:|
| NULL | 8% |
| 12~28자 | 12% |
| 30~100자 | 35% |
| 101~300자 | 30% |
| 301~700자 | 10% |
| 701~1,400자 | 5% |

## 적재와 검증

1. 전용 `mysql-perf`를 초기화하고 [init.sql](../../backend/src/test/resources/init.sql)로 Schema와 `content_type` 8개를 적재한다.
2. Generator의 `run --dry-run`으로 Schema·FK·기준값·`local_infile`을 확인한다.
3. `run`으로 CSV를 생성·적재하고, `verify`로 아래 검증 수치를 수집한다.
4. 실제 검증 결과와 manifest를 보관하고 기준을 만족한 뒤 워밍업을 시작한다.

| 검증 대상 | 확인할 내용 |
|---|---|
| 규모·무결성 | `place` 600만 건, 기준 ID 8개, FK / UNIQUE / NOT NULL |
| 유형·지역 | 유형·지역·Hotspot별 Count와 기대 분포 |
| 문자열 | description NULL·길이 분포, 희귀 토큰과 시나리오 요청의 매칭 Count |
| 좌표 | 허용 범위와 Hotspot 군집 분포 |
| 날짜 | `src_created_at <= src_updated_at` |

Weighted random의 기대 비율과 실제 비율 차이는 적재 구현에서 정한 허용 오차로 검증한다.
이 허용 오차는 Before / After에 서로 다른 데이터를 사용해도 된다는 의미가 아니다.
`verify`는 생성 namespace의 수치를 출력하며 분포 허용 오차를 자동 판정하지 않는다.
전체 `place` Count, Hotspot 분포, 시나리오 조건별 매칭 Count도 별도 확인한다.
수동 검증은 [SQL 템플릿](../../mock_data/validate_place_mock.sql)을 사용하며 seed 변경 시 namespace 범위를 맞춘다.

## 로컬 실행 예시

저장소 루트의 PowerShell에서 Python 3.10 이상과 MySQL CLI를 사용한다.
[환경변수 예시](../../perf/.env.example)를 `perf/.env`로 복사하고, 아래 포트·사용자를 해당 파일과 동일하게 맞춘다.
[성능 Compose](../../docker-compose.performance.yml)는 새 DB 볼륨의 첫 기동에 `init.sql`을 적용하며 `local_infile=1`을 설정한다.
기존 볼륨에서 `up`만 다시 실행하면 초기화되지 않는다. 재사용 시 [로컬 DB 초기화](#로컬-db-초기화)를 수행한다.
Before / After 모두 빈 `place`와 초기 AUTO_INCREMENT에서 시작해야 한다.

```powershell
docker compose --env-file perf/.env -f docker-compose.performance.yml up -d mysql-perf
```

DB가 healthy이고 기준 Schema·행이 준비된 뒤 실행한다. 비밀번호는 `perf/.env`의 `PERF_MYSQL_PASSWORD`에서 읽는다.
같은 이름의 프로세스 환경변수가 있으면 그 값이 우선하므로 Compose와 일치시킨다.

```powershell
$experimentId = "experiment-001"
$phase = "before" # After는 DB 재초기화 후 "after"로 변경
$outputDir = "mock_data/generated/$experimentId/$phase"
$generatorArgs = @("--config", "mock_data/place_mock_config.json")
$datasetArgs = @("--profile", "real", "--seed", "3671", "--chunk-size", "500000", "--output-dir", $outputDir)
$databaseArgs = @("--database", "moonbackdb", "--host", "127.0.0.1", "--port", "3306", "--user", "moonback", "--password-env", "PERF_MYSQL_PASSWORD", "--env-file", "perf/.env")
python mock_data/generate_place_mock.py @generatorArgs run @datasetArgs @databaseArgs --dry-run
if ($LASTEXITCODE -ne 0) { throw "Dataset 사전 검사 실패" }
python mock_data/generate_place_mock.py @generatorArgs run @datasetArgs @databaseArgs
if ($LASTEXITCODE -ne 0) { throw "Dataset 적재 실패" }
python mock_data/generate_place_mock.py @generatorArgs verify @datasetArgs @databaseArgs | Tee-Object -FilePath "$outputDir/verify.txt"
if ($LASTEXITCODE -ne 0) { throw "Dataset 검증 수집 실패" }
```

MySQL CLI가 PATH에 없으면 `$databaseArgs`에 `--mysql-bin`과 실행 파일 경로를 추가한다.
`small` 1만 건은 실행 점검용이며 `real` 성능 비교에 섞지 않는다.
같은 적재의 실패 재개에만 동일 설정·출력 경로와 `--resume`을 사용한다. 새 After에는 Before manifest를 재사용하지 않는다.

## 로컬 DB 초기화

아래 명령은 성능 Compose의 `mysql-perf` 안에서 `init.sql`을 다시 적용해 테스트 테이블과 기준 행을 재생성한다.
해당 DB의 기존 데이터·추가 인덱스는 삭제된다. 개발·운영 DB에서 사용하지 않는다.
측정을 중지하고 실행하며, 실패 시 다음 단계로 진행하지 않는다.

```powershell
docker compose --env-file perf/.env -f docker-compose.performance.yml stop backend frontend
if ($LASTEXITCODE -ne 0) { throw "측정 대상 중지 실패" }
docker compose --env-file perf/.env -f docker-compose.performance.yml exec -T mysql-perf sh -c 'mysql --user=root --password="$MYSQL_ROOT_PASSWORD" < /docker-entrypoint-initdb.d/001-schema.sql'
if ($LASTEXITCODE -ne 0) { throw "성능 DB 초기화 실패" }
```

초기화는 AUTO_INCREMENT도 기준 상태로 되돌린다. After에는 의도한 Schema / Index Patch만 다시 적용한다.
새 출력 경로로 위 적재·검증 절차를 수행한 후 backend·frontend를 기동하고 워밍업한다.
DB 초기화는 Host에 저장된 이전 manifest·검증 기록을 삭제하지 않는다.

## 재현성과 버전

- Before / After는 같은 생성 코드·설정·Python 버전과 위 seed/profile/chunk를 사용한다. 행별 seed로 생성 값을 재현한다.
- 빈 테이블과 같은 AUTO_INCREMENT에서 같은 순서로 적재해 `place_id`와 정렬·페이지 결과를 유지한다.
- DB 기본값인 `created_at`·`updated_at`은 재적재 시 달라질 수 있다. 이 시나리오의 검색 조건에는 사용하지 않는다.
- 각 단계의 `*.manifest.json`과 `verify.txt`를 보고서에 연결한다. manifest의 `config_sha256`·`fingerprint`를 비교하고 생성 코드 버전은 별도 기록한다.
- 성공한 chunk CSV는 기본 삭제되므로 manifest는 데이터 Snapshot 자체가 아니다. CSV 보관이 필요하면 `run --keep-files`를 사용한다.
- 생성 설정·Schema·데이터 분포가 바뀌면 새 Dataset Version을 부여한다.
- 실험 Patch의 Index / Schema 변경은 기준 Dataset을 복원한 뒤 적용하고, 데이터 자체는 유지한다.
- 데이터가 바뀌어야 하는 실험은 별도 Dataset Version과 Scenario로 분리한다.

## 남은 자동화 항목

- Orchestrator의 전용 DB 초기화·healthy 대기·명령 실행·부분 적재 실패 처리
- 분포 허용 오차와 자동 판정, 전체 Count·Hotspot·시나리오 매칭 검증
