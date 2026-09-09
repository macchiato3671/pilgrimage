# Place Search Performance Scenario

## 1. 목적

`perf-dataset-v1`의 Place 데이터를 대상으로 검색 API의 변경 전후 성능을 비교한다.

이 Scenario는 로컬 Docker Performance Environment에서 동일한 Dataset과 동일한 요청 조건으로 다음 지표를 반복 측정한다.

- 응답시간
- 처리량
- p95 / p99
- Error Rate
- Case별 성능 변화

모든 요청은 조회성 `GET` 요청으로 구성하며, 테스트 중 Dataset을 변경하지 않는다.
Baseline과 After는 같은 Scenario Version, 같은 부하, 같은 입력값으로 실행한다.

## 2. 실행 계약

- Target Base URL: 기본값 `http://localhost`
- Gateway: 로컬 Docker Performance Environment의 nginx
- Database: 개발용 DB가 아닌 `mysql-perf`
- Dataset: `perf-dataset-v1`
- 실행 단위: Warm-up 후 Baseline 또는 After Measurement
- 반복: Baseline과 After를 가능하면 각각 3회 이상 실행
- Warm-up 결과: Measurement 통계에서 제외

Harness는 각 Run 전에 Docker 환경과 Dataset 상태를 확인하고, Dataset Version과 실제 Row Count를 Report에 기록한다.
Target URL, Dataset, Scenario, Docker Resource, Build 환경·설정은 고정한다. Before / After의 Git SHA와 Image Tag 차이는 의도한 Patch로 한정한다.

## 3. Target API

~~~text
GET /api/v1/places/search
~~~

현재 Controller의 검색 조건은 다음 중 하나 이상을 포함해야 한다.

- `keyword`
- `contentTypeId`
- `latitude`와 `longitude` 쌍

아무 검색 조건 없이 호출하는 요청은 정상 성능 측정 대상에서 제외한다.

## 4. Request Parameters

| Parameter | Type | Required | Default / 제한 | 설명 |
|---|---|---:|---|---|
| `keyword` | String | 조건부 | 없음 | `name`, `address`, `description`에 대한 부분 검색 |
| `contentTypeId` | Integer | 조건부 | 없음 | `content_type_id` 일치 검색 |
| `latitude` | Double | 조건부 | `longitude`와 함께 사용 | Place 위도 조건 |
| `longitude` | Double | 조건부 | `latitude`와 함께 사용 | Place 경도 조건 |
| `radiusKm` | Double | 위치 검색 시 | 3.0, 0보다 커야 함 | 좌표 조건의 범위 |
| `page` | Integer | 아니오 | 0, 0 이상 | 페이지 번호 |
| `size` | Integer | 아니오 | 10, 1~50 | 페이지 크기 |

예시 요청:

~~~text
GET /api/v1/places/search?keyword=공원&page=0&size=10
GET /api/v1/places/search?contentTypeId=12&page=0&size=10
GET /api/v1/places/search?latitude=37.4979&longitude=127.0276&radiusKm=3.0&page=0&size=10
~~~

좌표 검색은 실제 원형 반경이 아니라 다음과 같은 위도·경도 사각 범위 조건을 사용한다.

~~~text
ABS(place.latitude - latitude) <= radiusKm / 111
ABS(place.longitude - longitude) <= radiusKm / 111
~~~

검색 목록은 `place_id ASC`로 정렬한 뒤 `LIMIT/OFFSET` pagination을 적용한다.

## 5. Dataset

- Dataset: `perf-dataset-v1`
- 대상 Entity: `place`
- 적재 목표 규모: 6,000,000건
- Content Type 기준 데이터: `content_type` 8개
- Dataset 상세: [`perf-dataset-v1.md`](../datasets/perf-dataset-v1.md)

6,000,000건은 Dataset 정의의 기준 규모다. 각 Experiment에서는 로컬 `mysql-perf`에 적재한 실제 Row Count와 분포 검증 결과를 기록한다.
개발용 DB의 현재 상태를 Dataset Snapshot으로 간주하지 않는다.

## 6. Test Cases

### 6.1 Baseline workload

아래 입력값은 매 실행마다 동일하게 사용한다. 요청 순서를 매번 바꾸거나 임의의 keyword를 생성하지 않는다.

| Case | 목적 | 요청 조건 | 기본 비중 |
|---|---|---|---:|
| PS-01 | 일반 keyword 검색 | `keyword=공원&page=0&size=10` | 30% |
| PS-02 | Content Type 필터 검색 | `contentTypeId=12&page=0&size=10` | 25% |
| PS-03 | Hotspot 위치 검색 | `latitude=37.4979&longitude=127.0276&radiusKm=3.0&page=0&size=10` | 25% |
| PS-04 | 복합 조건 검색 | `keyword=문화&contentTypeId=14&latitude=37.5563&longitude=126.9236&radiusKm=3.0&page=0&size=10` | 10% |
| PS-05 | 깊은 페이지 조회 | `contentTypeId=12&page=100&size=50` | 10% |

Workload 비중은 한 번의 측정 구간에서 목표 비중에 가깝게 유지한다. 각 Case의 실제 응답 건수는 Dataset의 weighted random 결과와 DB 상태에 따라 달라질 수 있으므로 고정하지 않는다.

### 6.2 Dataset 특성과 Case 연결

- `PS-01`은 이름·주소·description에 포함된 `공원` 후보를 대상으로 한다. `공원`은 Dataset에서 비교적 빈번한 이름 토큰이다.
- `PS-02`는 `content_type_id=12`의 Place만 대상으로 한다. 이 유형의 설정 비율은 24%다.
- `PS-03`은 수도권 Hotspot 중심 좌표를 사용한다. Dataset 좌표는 전국 균등 분포가 아니라 Hotspot 주변에 군집되어 있다.
- `PS-04`는 keyword, Content Type, 좌표를 동시에 적용한다. `content_type_id=14`의 설정 비율은 15%다.
- `PS-05`는 `place_id` 정렬과 OFFSET pagination을 함께 사용한다.
- 희귀 keyword인 `별빛누리` 검색은 기본 workload에는 포함하지 않고 별도 탐색 Case로 실행한다. 해당 토큰의 설정 확률은 0.02%이며 실제 매칭 Row Count는 `mysql-perf`에서 확인한다.

### 6.3 선택 Case

데이터 분포별 응답을 확인해야 할 때 다음 Case를 별도로 실행한다.

~~~text
GET /api/v1/places/search?keyword=별빛누리&page=0&size=10
GET /api/v1/places/search?contentTypeId=32&page=0&size=50
GET /api/v1/places/search?latitude=35.1631&longitude=129.1635&radiusKm=1.0&page=0&size=10
~~~

선택 Case의 결과 유무나 정확한 `totalElements`는 사전에 고정하지 않는다. 이 Case는 기본 Baseline 수치에 섞지 않고 별도 결과로 기록한다.

## 7. Expected Result

정상 Case의 공통 검증 기준은 다음과 같다.

- HTTP Status: `200`
- 응답에 `places` 목록과 `page` 정보가 존재
- `places` 개수는 요청한 `size` 이하
- `page.number`, `page.size`, `page.totalElements`, `page.totalPages`가 요청 및 Count 결과와 일치
- `PS-02`, `PS-04`의 모든 결과는 요청한 `contentTypeId`와 일치
- 위치 조건이 있는 Case의 결과는 Query가 적용한 위도·경도 범위 조건을 만족
- `PS-01`, `PS-04`의 결과는 `name`, `address`, `description` 중 하나에 keyword가 포함되는지 확인
- Place의 `images` 배열은 DB의 `place_img` 상태에 따라 비어 있을 수 있음

정확한 Row Count를 Assertion으로 고정하지 않는다. 성능 비교에 필요한 것은 각 실행에서 동일한 Dataset과 요청 조건을 사용했는지 여부다.

## 8. Metrics와 Evidence

### Client / k6

- 총 요청 수
- RPS
- 평균 응답시간
- p50, p95, p99
- 최대 응답시간
- HTTP Error Rate
- HTTP Status별 응답 수
- Case별 응답시간과 Error Rate

### Prometheus

같은 Measurement 구간의 다음 상태를 API로 조회한다.

- Application CPU와 Memory
- JVM Heap, GC
- DB CPU와 Memory
- Active / Idle Connection 및 HikariCP 대기

### Tempo

가능한 경우 같은 요청의 다음 구간을 확인한다.

- 전체 요청 시간
- JDBC 시간
- 외부 HTTP I/O 시간
- 내부 Service 구간

### MySQL

- Place 검색 Query 실행시간
- Slow Query
- `COUNT` Query와 목록 Query의 실행 계획
- `EXPLAIN ANALYZE` 결과
- DB Buffer Pool 상태

Metric 하나만으로 병목을 확정하지 않는다. k6, Prometheus, Tempo, SQL Evidence를 연결해 병목 가설을 작성한다.
JFR은 Prometheus에서 CPU 병목이 확인된 경우에만 선택적으로 수집한다.

## 9. Load Profile

초기 Baseline과 After는 재현성을 우선해 다음 고정 부하로 실행한다.

| 단계 | 시간 | 부하 |
|---|---:|---|
| Warm-up | 1분 | 1 VU |
| Measurement | 3분 | 10 VU |
| Cool-down | 1분 | 1 VU |

- Case 비중은 PS-01 30%, PS-02 25%, PS-03 25%, PS-04 10%, PS-05 10%을 사용한다.
- 모든 Case는 같은 Dataset, 같은 API Build, 같은 DB 상태에서 실행한다.
- 비교 측정에서는 VU, Duration, Case 비중, 요청 입력값을 변경하지 않는다.
- Baseline과 After는 가능하면 각각 3회 이상 반복한다.
- 각 Run의 결과와 Scenario에 정의된 대표값을 모두 기록한다.
- 부하 한계 측정이 필요하면 Before / After Gate와 분리해 1 → 5 → 10 → 20 VU 단계로 실행한다.
- 부하 한계 측정 결과를 Baseline 대표값과 섞지 않는다.

## 10. Baseline 및 비교 기록

Baseline과 After 결과에는 다음 정보를 함께 기록한다.

- Experiment ID
- 측정 일시
- Target Base URL: local Docker Performance Environment
- Application commit 또는 image tag
- Experiment branch와 기준 Git SHA
- Dataset ID와 Version: `perf-dataset-v1`
- Dataset의 실제 `place` Row Count와 주요 분포 검증 결과
- MySQL 버전과 DB 인스턴스 정보
- Docker Resource, JVM, Application, HikariCP 설정
- VU, 실행 시간, Warm-up 시간
- Test Case 비중과 요청 입력값
- Run별 p50/p95/p99, RPS, Error Rate
- Prometheus / Tempo / SQL Evidence 위치 또는 식별자
- 측정 중 Schema, Index, DB 설정 변경 여부

Before / After 비교는 같은 Snapshot, 같은 Load Profile, 같은 Evidence 수집 조건으로 수행한다.
Schema 또는 Index를 변경한 경우에는 After 측정 전에 기준 Docker 환경과 Dataset을 먼저 재초기화하고, 의도한 Patch의 Schema / Index 변경만 적용한다. Data 상태가 바뀐 경우에는 같은 Dataset Version으로 다시 적재한다.

이 Scenario의 성공 기준은 운영 환경의 절대 RPS가 아니라 같은 로컬 조건에서의 목표 Metric 개선, 오류 허용 범위, 회귀 여부다.
구체적인 Threshold는 Scenario별 설정값으로 관리한다.
