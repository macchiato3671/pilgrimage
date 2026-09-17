# Place Search Performance Scenario

[perf-dataset-v1](../datasets/perf-dataset-v1.md)의 `place` 600만 건으로 검색 API의 변경 전후를 비교한다.
환경은 [Performance Test Profile](../profiles/performance-test-profile.md), 절차·분석·판정은 [공통 계약](../README.md)을 따른다.

## 대상과 검색 의미

```text
GET /api/v1/places/search
```

Target Base URL 기본값은 `http://localhost`(80)이며 `PERF_HTTP_PORT` 변경 시 해당 포트에 맞춘다.
요청은 `keyword`, `contentTypeId`, 위도·경도 쌍 중 하나 이상을 포함해야 한다.
모든 Case는 조회 요청이며 측정 중 데이터를 변경하지 않는다.

| 파라미터 | 의미·제한 |
|---|---|
| `keyword` | `name`, `address`, `description` 중 하나에 포함되는 문자열 |
| `contentTypeId` | 유형 ID 일치 |
| `latitude`, `longitude` | 위치 조건, 두 값을 함께 사용 |
| `radiusKm` | 기본 3.0, 양수 |
| `page` | 기본 0, 0 이상 |
| `size` | 기본 10, 1~50 |

현재 위치 검색은 아래 사각 범위이며 원형 거리 검색이 아니다.
현재 목록은 `place_id ASC` 정렬과 `LIMIT/OFFSET`으로 조회한다. 성능 Patch에서도 검색 결과·정렬·페이지 의미를 유지한다.

```text
ABS(place.latitude - latitude) <= radiusKm / 111
ABS(place.longitude - longitude) <= radiusKm / 111
```

## 고정 Case

| Case | 목적 | 요청 조건 | 비중 |
|---|---|---|---:|
| PS-01 | 일반 keyword | `keyword=공원&page=0&size=10` | 30% |
| PS-02 | 유형 필터 | `contentTypeId=12&page=0&size=10` | 25% |
| PS-03 | Hotspot 위치 | `latitude=37.4979&longitude=127.0276&radiusKm=3.0&page=0&size=10` | 25% |
| PS-04 | 복합 조건 | `keyword=문화&contentTypeId=14&latitude=37.5563&longitude=126.9236&radiusKm=3.0&page=0&size=10` | 10% |
| PS-05 | 깊은 페이지 | `contentTypeId=12&page=100&size=50` | 10% |

입력값과 요청 순서는 실행마다 동일하게 사용하고 측정 구간의 Case 비중을 목표에 가깝게 유지한다.
임의 keyword나 별도 탐색 Case는 기본 비교 수치에 섞지 않는다.

## 부하

| 단계 | 시간 | 부하 |
|---|---:|---|
| Warm-up | 1분 | 1 VU |
| Measurement | 3분 | 10 VU |
| Cool-down | 1분 | 1 VU |

- 측정 통계는 Measurement 구간만 사용한다.
- Baseline과 After는 가능하면 각각 3회 이상 반복하며 횟수·부하·입력값을 동일하게 유지한다.
- 각 Run의 원본 결과, 전체·Case별 p50/p95/p99, RPS, 오류율과 대표값을 기록한다.
- 고정값을 바꾸면 Scenario Version 또는 실험 설정으로 기록한다.

## 정확성 검증

- HTTP `200`, 응답에 `places`와 `page` 존재, 목록 개수는 요청 `size` 이하
- `page.number`, `page.size`, `page.totalElements`, `page.totalPages`가 요청·Count 결과와 일치
- 결과는 `place_id ASC` 정렬 및 요청한 페이지에 부합
- PS-02·PS-04는 모든 결과의 `contentTypeId`가 요청과 일치
- PS-03·PS-04는 모든 결과가 위도·경도 범위 조건을 만족
- PS-01·PS-04는 `name`, `address`, `description` 중 하나에 keyword 포함
- `images`는 `place_img` 상태에 따라 빈 배열 허용

문서에 정확한 매칭 Row Count를 상수로 고정하지 않는다.
적재한 고정 Dataset의 실제 Count와 검색 결과를 기준으로 검증한다.

## 성능 판정 설정

다음 항목은 아직 미확정이다. 측정 전에 설정으로 확정하고 보고서에 적용값을 남긴다.

| 항목 | 확정할 내용 |
|---|---|
| 목표 지표 | 전체 또는 특정 Case의 지표와 최소 개선폭 |
| 오류율 | 허용 Threshold |
| 처리량 | RPS 감소 허용폭 |
| 꼬리 지연 | p99 회귀 허용폭 |
| 반복 측정 | 양쪽 Run 수, 대표값 집계 방식, 제외 규칙, 개선 방향 일관성 판정 기준 |

기능 Gate와 위 성능 기준을 모두 만족해야 PASS다.
비교에는 같은 Dataset Snapshot·부하·관측 조건을 사용한다.
결과와 근거는 [보고서 양식](../reports/performance-report-template.md)에 기록한다.
