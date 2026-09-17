# Place Mock Data Generator

## 목적

이 도구는 성능 테스트용 "place" 데이터만 생성하고 적재한다. "drama",
"scene", "member", "wishlist", "travel_plan", "place_img" 데이터는 생성하지
않는다.

기준 스키마는 backend/src/test/resources/init.sql의 "place"와
"content_type"이다.

적재 컬럼은 다음 순서로 고정되어 있다.

    content_id, content_type_id, name, address, latitude, longitude,
    description, src_created_at, src_updated_at

"place_id"는 AUTO_INCREMENT에 맡기고 "created_at", "updated_at"은 MySQL
기본값에 맡긴다. "description"은 빈 CSV 값을 NULL로 변환한다.

`place.content_id`는 `init.sql`과 Place DTO가 모두 `INT` 계약을 사용한다.
Mock 데이터는 이 타입을 지키면서도 기존 원천 콘텐츠 번호와 충돌하지 않도록
설정된 높은 숫자 범위를 seed별 namespace로 나누어 사용한다. 숫자 범위가
이미 사용 중이면 Generator가 적재를 중단한다.

## 분석 결과

- 현재 Place 조회는 JPA Repository가 아니라 MyBatis PlaceMapper.xml을
  사용한다.
- 검색은 "name", "address", "description"의 %keyword% 부분 검색,
  "content_type_id" equality, 위도/경도 차이의 사각 범위 조건을 사용한다.
- 근처 장소 조회는 "content_type_id"와 위도/경도 사각 범위를 사용하고
  거리 계산 후 정렬한다.
- "place"에는 PRIMARY KEY와 "content_id" UNIQUE만 있고 카테고리/좌표/검색
  전용 인덱스는 현재 스키마에 없다. Generator는 인덱스를 임의로 추가하지
  않고 현재 조회 방식에 의미 있는 데이터 분포를 만든다.

## content_type_id

init.sql의 정적 기준 행을 그대로 설정에 반영했다.

    12 관광지
    14 문화시설
    15 축제공연행사
    25 여행코스
    28 레포츠
    32 숙박
    38 쇼핑
    39 관광지

각 Place CSV 행에는 위 ID 중 하나가 반드시 들어간다. "content_type" 테이블
자체를 Mock Data로 대량 생성하지는 않는다. 적재 전 FK와 ID/이름을 검증하며,
기준 행이 없거나 이름이 다르면 적재를 중단한다.

## 데이터 분포

- 기본 seed는 3671이며, 행별 seed를 계산하므로 chunk를 나누거나 재생성해도
  같은 설정의 같은 행이 생성된다.
- 기본 profile은 small=10,000, medium=500,000, real=6,000,000이다.
- 기본 chunk는 500,000건이다.
- 수도권 52%, 부산/울산 12%, 대구/경북 9%, 대전/충청 10%,
  광주/전라 8%, 강원 5%, 제주 4% 가중치를 사용한다.
- 각 지역에는 여러 hotspot 중심과 Gaussian 표준편차가 설정되어 있다.
  수도권 상권은 좁은 표준편차와 많은 행으로, 지방 및 저밀도 hotspot은
  넓은 표준편차와 적은 행으로 만든다.
- 34개 hotspot에 약 201개의 가중 주소 후보를 두었다. 선택한 hotspot의
  좌표 밀도는 유지하면서 시/군/구/읍/면/동 후보를 분산해 주소 검색어의
  결과 건수 차이를 만들고, 선택된 주소 후보와 좌표가 같은 도시권에
  있도록 구성한다.
- "공원"은 자주, "문화"는 중간 빈도, "별빛누리"는 희귀 빈도로 이름에
  등장하도록 구성했다.
- description은 NULL/짧은 값부터 1,400자 값까지 여러 bucket으로 나누며,
  TEXT 최대 바이트를 넘지 않도록 60,000 bytes에서 제한한다.
- src_created_at <= src_updated_at를 보장하고 날짜를 2016~2026 기간에
  분산한다.
- content_id는 백엔드의 int 계약에 맞춘 숫자형 deterministic namespace를
  사용한다. 기본 seed 3671의 범위는 1,838,000,000~1,843,999,999이며,
  같은 숫자 namespace에 기존 행이 있으면 기본 동작은 중단한다.

## 로컬 실행

저장소 루트에서 Python 3.10 이상과 MySQL CLI를 사용한다. 별도 pip 패키지는 필요 없다.
MySQL CLI가 PATH에 없으면 `--mysql-bin`으로 실행 파일 경로를 지정한다.

하네스의 준비·적재·검증 명령은 [Dataset 실행 절차](../.harness/datasets/perf-dataset-v1.md#로컬-실행-예시)에 모아 둔다.

| 항목 | 로컬 하네스 기준 |
|---|---|
| Compose | `docker-compose.performance.yml` |
| DB 서비스 / 데이터베이스 | `mysql-perf` / `moonbackdb` |
| Host에서 접속 | `127.0.0.1:3306` (기본값) |
| 인증 설정 | `perf/.env`의 `PERF_MYSQL_USER`, `PERF_MYSQL_PASSWORD` |
| 생성 기준 | seed `3671`, profile `real`, chunk `500000` |
| 결과 경로 | `mock_data/generated/<experiment-id>/<before 또는 after>/` |

Host에서 실행하는 Generator는 공개 포트 `3306`에 접속한다.
Docker 내부의 backend는 `mysql-perf:3306`을 사용한다.
포트·사용자 변경 시 실행 인자도 맞춘다. Generator가 `PERF_*` 접속 설정 전체를 자동으로 읽지는 않는다.

## 명령과 적재 방식

| 명령 | 동작 |
|---|---|
| `generate` | DB 연결 없이 CSV와 manifest 생성 |
| `run --dry-run` | DB Schema·FK·기준값·namespace·`local_infile` 사전 확인 |
| `run` | chunk별 CSV 생성과 `LOAD DATA LOCAL INFILE` 적재 |
| `load` | 기존 manifest가 가리키는 CSV 적재 |
| `verify` | 생성 namespace의 건수·분포·무결성 수치 출력 |

DB 명령에는 `--database`를 명시한다. 성능 Compose는 MySQL의 `local_infile=1`을 설정한다.
성공한 chunk CSV는 기본 삭제되고 manifest는 남는다. CSV를 보존하려면 `--keep-files`를 사용한다.
`load`에는 보존된 CSV가 필요하며 manifest만으로 데이터를 복원할 수 없다.

CSV만 생성하는 예시(PowerShell, 저장소 루트):

```powershell
python mock_data/generate_place_mock.py --config mock_data/place_mock_config.json generate --profile small --seed 3671 --chunk-size 1000 --output-dir mock_data/generated/csv-check
```

`small`은 실행 점검용이다. 실제 성능 비교에는 Dataset 계약의 `real`을 사용한다.
CSV와 MySQL 데이터가 각각 디스크 공간을 사용하므로 600만 건 적재 전에 여유 공간을 확인한다.

## 검증과 실패 재개

`verify`는 유형·주소 기반 지역·description 길이/NULL·좌표·중복·날짜 순서·이름 토큰 수치를 출력한다.
분포 허용 오차의 통과 여부는 자동 판정하지 않으므로 [Dataset 검증 기준](../.harness/datasets/perf-dataset-v1.md#적재와-검증)과 비교한다.
수동 확인에는 [검증 SQL](validate_place_mock.sql)을 사용하고 seed 변경 시 파일 상단 namespace 범위도 맞춘다.

같은 적재의 실패를 재개할 때만 코드·설정·seed·profile·count·chunk-size·출력 경로를 유지하고 `run --resume`을 사용한다.
완전히 적재된 chunk는 건너뛰며 부분 적재된 chunk는 자동 삭제하지 않고 중단한다.
부분 적재 상태를 초기화해 다시 시작한다면 이전 manifest를 보존하고 새 출력 경로를 사용한다.
Before에서 After로 넘어갈 때도 같은 데이터 생성 조건과 별도 출력 경로를 사용한다.

## 로컬 DB 초기화

Generator에는 기존 데이터를 삭제하거나 AUTO_INCREMENT를 초기화하는 명령이 없다.
[하네스의 전용 DB 초기화 절차](../.harness/datasets/perf-dataset-v1.md#로컬-db-초기화)를 사용한다.
초기화 후 기준 행·Schema를 확인하고 데이터를 재적재한다. 개발·운영 DB는 대상에서 제외한다.
