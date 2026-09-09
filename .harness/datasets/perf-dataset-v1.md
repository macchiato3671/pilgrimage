# Performance Dataset v1

`perf-dataset-v1`은 로컬 Docker Performance Environment의 `mysql-perf`에 반복 적재하는 고정 Dataset 계약이다.
운영 DB, 개발자의 일반 개발 DB, 특정 장비에 이미 남아 있는 DB 상태를 Dataset으로 간주하지 않는다.

Mock Data 생성 방식은 현재 브랜치가 아니라 `feat/place-mock-data` 브랜치의 `60c9db2` 커밋(`feat: place mock data generator`)의 실제 Generator와 설정을 기준으로 정리했다.
이 커밋은 Dataset 생성 규칙의 출처이며, 실험은 이 문서에 기록된 Version과 규칙을 사용해 로컬 DB를 초기화하고 다시 적재해야 한다.

이 문서에서 `생성`은 Generator가 직접 만드는 Entity를 의미한다. 기존 기준 데이터를 참조하거나 조회 과정에서만 사용하는 Entity는 생성 여부를 별도로 표시한다.

## 1. Entity Summary

| Entity | 역할 | 생성 규모 |
|---|---|---:|
| `place` | 장소 검색의 핵심 Entity | 기준 규모 6,000,000건 |
| `content_type` | Place의 콘텐츠 유형을 나타내는 FK 기준 Entity | Generator 미생성, 기준 행 8개 참조 |

Generator가 실제 Mock Row를 생성하는 Entity는 `place` 하나다. `content_type`은 Place 적재 전에 DB에 존재해야 하는 기준 Entity다.

## 2. Entity별 상세

### 2.1 Place

#### Entity 설명

`place`는 하나의 장소를 나타내는 핵심 Entity다. 현재 성능 테스트에서는 다음 조회의 기준 Table로 사용된다.

- `GET /api/v1/places/search`의 keyword·콘텐츠 유형·위치 검색

#### 주요 Field

| Field | 의미 |
|---|---|
| `place_id` | Place PK. DB `AUTO_INCREMENT`로 생성되며 현재 검색 결과의 정렬·페이지 순서에 사용 |
| `content_id` | 원천 콘텐츠 식별자. `UNIQUE`인 정수 값 |
| `content_type_id` | `content_type`과 연결되는 FK |
| `name` | 장소명. keyword 검색 대상 |
| `address` | 주소. keyword 검색 대상 |
| `latitude`, `longitude` | 장소 좌표. 위치·근처 검색 대상 |
| `description` | 장소 설명. NULL 가능하며 keyword 검색 대상 |
| `src_created_at` | 원천 데이터 생성 시각 |
| `src_updated_at` | 원천 데이터 수정 시각 |
| `created_at`, `updated_at` | DB가 관리하는 적재·수정 시각 |

#### 관계

- `content_type` 1 : N `place`
- Place 간 직접 관계는 Generator에 정의되어 있지 않다.

#### Mock Data 생성 방식

##### content_type_id 비율

| ID | 이름 | 기대 비율 |
|---:|---|---:|
| 12 | 관광지 | 24% |
| 14 | 문화시설 | 15% |
| 15 | 축제공연행사 | 7% |
| 25 | 여행코스 | 5% |
| 28 | 레포츠 | 12% |
| 32 | 숙박 | 13% |
| 38 | 쇼핑 | 10% |
| 39 | 관광지 | 14% |

각 Place의 `content_type_id`는 위 후보 중 하나로 선택되며, 해당 ID가 `content_type` Table에 존재해야 한다.

##### name

이름은 지역·hotspot·prefix·content type suffix를 조합해 만든다.

| 이름 형태 | 기대 비율 |
|---|---:|
| Hotspot + Prefix + Suffix | 38% |
| Region + Prefix + Suffix | 24% |
| Hotspot + Suffix | 20% |
| Prefix + Hotspot + Suffix | 12% |
| Region + Hotspot + Suffix | 6% |

- Prefix 후보: 푸른, 한빛, 바람, 솔빛, 느티, 새봄, 아트, 해안, 온누리, 별내, 마루, 초록
- 희귀 이름 토큰 `별빛누리`: 0.02%
- 이름은 최대 255자로 제한된다.
- Suffix 후보는 `content_type_id`에 따라 달라진다.

| Content Type | Suffix 후보 |
|---|---|
| 12 관광지 | 공원, 전망대, 해변, 산, 광장, 유적지, 수목원 |
| 14 문화시설 | 박물관, 미술관, 문화회관, 공연장, 도서관 |
| 15 축제공연행사 | 축제, 문화제, 페스티벌, 행사장 |
| 25 여행코스 | 둘레길, 탐방코스, 문화탐방길, 생태코스, 도보여행길 |
| 28 레포츠 | 체육공원, 캠핑장, 골프장, 레저센터, 스포츠파크 |
| 32 숙박 | 호텔, 모텔, 게스트하우스, 리조트, 한옥스테이 |
| 38 쇼핑 | 시장, 백화점, 아울렛, 쇼핑몰, 상점가 |
| 39 관광지 | 공원, 전망대, 해변, 산책로, 명소, 유적지 |

##### address

- 7개 지역권, 34개 hotspot, 총 201개 주소 후보를 사용한다.
- 지역과 hotspot은 각각 weighted random으로 선택된다.
- 주소 형식은 다음과 같다.

~~~text
{도시} {구역} {동네} {1~999}-{1~99}
~~~

- 도시·구역·동네는 선택된 hotspot의 기본값 또는 주소 후보에서 가져온다.
- 주소 숫자 부분은 random이다.
- 따라서 주소 문자열은 실제 운영 주소가 아닌 합성 Mock 값이다.

##### latitude / longitude

- 전체 허용 범위: 위도 `33.0~38.6`, 경도 `126.0~129.7`
- 각 지역에는 여러 hotspot 중심 좌표가 있다.
- 좌표는 hotspot 중심 주변의 Gaussian random 값으로 생성된다.
- 지역 bounds 밖으로 벗어난 값은 다시 선택하며, 반복 후에는 hotspot 중심 좌표를 사용한다.
- 좌표는 전국 균등 분포가 아니라 hotspot 주변에 군집된다.
- CSV에는 위도·경도가 소수점 7자리로 기록된다.

지역별 기대 비율은 다음과 같다.

| 지역권 | 기대 비율 | Hotspot 수 | 주소 후보 수 |
|---|---:|---:|---:|
| 수도권 | 52% | 8 | 48 |
| 부산/울산 | 12% | 4 | 23 |
| 대구/경북 | 9% | 4 | 24 |
| 대전/충청 | 10% | 5 | 30 |
| 광주/전라 | 8% | 5 | 30 |
| 강원 | 5% | 5 | 29 |
| 제주 | 4% | 3 | 17 |

##### description

Description은 다음 비율로 NULL 또는 길이 구간을 선택한다.

| 값 구간 | 기대 비율 |
|---|---:|
| NULL | 8% |
| 12~28자 | 12% |
| 30~100자 | 35% |
| 101~300자 | 30% |
| 301~700자 | 10% |
| 701~1,400자 | 5% |

- NULL이 아닌 값은 장소명·주소·content type·지역·hotspot 관련 문장을 반복해 목표 길이를 채운다.
- UTF-8 기준 최대 60,000 bytes로 제한된다.
- 실제 NULL 비율과 길이별 Count는 DB에서 확인한다.

##### 날짜

- `src_created_at`: 2016-01-01부터 2025-12-31까지 random
- `src_updated_at`: 생성 시각 이후 최대 730일, 최종 상한 2026-06-30
- `src_created_at <= src_updated_at` 관계를 보장한다.

##### 조회에서 사용되는 Place 조건

현재 MyBatis Query는 다음 Field를 사용한다.

- `name`, `address`, `description`: `LIKE '%keyword%'` 부분 검색
- `content_type_id`: 유형 equality filter
- `latitude`, `longitude`: 입력 좌표와의 범위 filter
- `place_id`: `ORDER BY place_id ASC` 및 pagination
- `content_type`: Place 검색 결과에 유형명 제공


### 2.2 Content Type

#### Entity 설명

`content_type`은 Place의 콘텐츠 유형을 나타내는 기준 Entity다. Place 검색 결과에 유형 ID와 유형명을 제공하고, `place.content_type_id`의 FK 대상이 된다.

#### 주요 Field

| Field | 의미 |
|---|---|
| `content_type_id` | 콘텐츠 유형 식별자 및 Place FK 대상 |
| `name` | 콘텐츠 유형 이름 |

#### 관계

- `content_type` 1 : N `place`
- Generator가 content type 기준 Row를 대량 생성하지 않는다.
- Place를 적재하려면 설정에 정의된 기준 ID와 이름이 DB에 존재해야 한다.

#### Mock Data 생성 방식

Generator는 이 Entity를 생성하지 않고 기존 기준 데이터를 검증·참조한다.

- 참조 ID: 12, 14, 15, 25, 28, 32, 38, 39
- 기준 Row 수: 8개
- Place와의 연결: 각 Place가 위 ID 중 하나를 FK로 참조
- 실제 ID별 Place 연결 개수: DB에서 확인 필요

## 3. Harness 적재 계약

### 3.1 Canonical Version

- Dataset ID: `perf-dataset-v1`
- 기준 규모: `place` 6,000,000건
- 기준 Entity: `content_type` 8개, `place` 6,000,000건
- 생성 규칙의 출처: `feat/place-mock-data` branch의 `60c9db2`
- 실행 환경: 로컬 Docker의 `mysql-perf`

Generator 설정, Schema, 데이터 분포를 바꾸면 기존 Version을 덮어쓰지 않는다.
변경된 Dataset은 새 Version으로 만들고, Before / After 양쪽이 같은 Version을 사용하도록 한다.

### 3.2 초기화와 적재 순서

각 Experiment는 가능하면 다음 순서로 `mysql-perf`를 준비한다.

```text
mysql-perf 초기화
→ Schema 적용
→ content_type 기준 Row 8개 확인 또는 적재
→ place Dataset 생성 및 적재
→ FK / UNIQUE / NOT NULL 검증
→ Row 수와 분포 검증
→ Dataset Snapshot 정보 기록
```

Dataset을 개발용 DB나 다른 Experiment의 잔존 Volume에 이어서 적재하지 않는다.
Index 변경이나 Schema 실험을 수행한 뒤에는 기준 환경과 Dataset을 다시 초기화하고, Patch에 포함된 의도한 Index / Schema 변경만 적용한 다음 After를 측정한다.

### 3.3 Dataset 검증 항목

적재 후에는 다음 값을 확인하고 Report에 기록한다.

- `place` 전체 Row Count가 기준 규모와 일치하는지
- `content_type` 기준 Row가 8개이고 참조 ID가 모두 존재하는지
- `content_type_id`별 Row Count와 기대 비율
- 지역권·Hotspot별 Row Count와 기대 비율
- `description` NULL 비율과 길이 구간별 분포
- `별빛누리` 희귀 토큰의 실제 매칭 Row Count
- 위도·경도 허용 범위와 Hotspot 군집 분포
- `src_created_at <= src_updated_at` 조건
- `content_id` UNIQUE 및 `content_type_id` FK 무결성

Generator의 weighted random 특성 때문에 각 값이 기대 비율과 완전히 일치할 필요는 없다.
허용 오차와 검증 방식은 Dataset 적재 구현에서 설정하고, Experiment마다 실제 결과를 기록한다.

### 3.4 Before / After 불변 조건

다음 항목이 달라지면 같은 Performance 비교로 판정하지 않는다.

- Dataset Version 또는 생성 규칙
- `place` Row Count
- Content Type, 지역, Hotspot 분포
- Keyword 후보와 희귀 토큰 분포
- `description` NULL 비율과 길이 분포
- 좌표 분포
- Pagination 대상 규모

성능 개선을 위해 Index, Schema, Query를 변경하는 Experiment라도 Dataset Version과 데이터 자체는 유지한다.
데이터 자체를 바꾸어야 한다면 별도 Dataset Version과 별도 Scenario로 분리한다.

## 4. 구현 전 확정이 필요한 항목

이 문서는 Dataset의 규칙과 검증 계약을 정의한다. 실제 Harness 구현 단계에서 다음을 구체화해야 한다.

- Dataset 생성 또는 적재 명령
- Schema 적용 위치와 순서
- `mysql-perf` 초기화 방식
- 대용량 CSV 또는 적재 파일의 보관 위치
- 분포 검증 Query와 허용 오차
- Dataset Snapshot / Manifest 형식
- 적재 실패 시 정리와 재시도 방식
