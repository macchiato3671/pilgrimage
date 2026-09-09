# Performance Analysis Flow

## 전체 Experiment 흐름

Harness는 로컬 격리 환경에서 Before / After를 같은 조건으로 수행한다.

```text
1. Experiment 설정과 Git 실험 영역 준비
2. 로컬 Docker Performance Environment 초기화
3. mysql-perf Schema 및 고정 Dataset 적재
4. Dataset Row 수·분포 검증
5. Warm-up
6. Baseline k6 실행을 여러 회 반복
7. k6 결과와 Metric / Trace / SQL Evidence 수집
8. AI가 Evidence를 연결해 병목 가설 작성
9. 개선 대안과 Trade-off 검토
10. 영향 범위가 작은 Patch 적용
11. Test / Build / Validation 실행
12. Docker 환경과 Dataset 재초기화
13. 동일한 Warm-up과 k6 Scenario로 After 반복 실행
14. 대표값과 Evidence를 Before / After로 비교
15. Functional Gate와 Performance Gate 판정
16. PASS면 실험 branch에 commit, FAIL이면 commit하지 않고 변경 폐기
17. 성공·실패 결과 모두 Report로 기록
```

실험 대상은 현재 개발 작업 영역이 아니라 Performance 전용 worktree와 그 안에서 빌드된 로컬 Docker 환경이다.
자동 merge는 하지 않으며, PASS 후 merge 여부는 사람이 결정한다.

## 측정과 Evidence 수집

### 1. k6 결과

k6 결과로 어떤 Scenario와 API Case가 느린지 먼저 확인한다.
다음 값을 Warm-up과 분리해 기록한다.

- Case별 응답시간과 Error Rate
- p50, p95, p99
- RPS와 총 요청 수
- HTTP Status별 응답 수

### 2. Prometheus Metric

같은 측정 구간의 Prometheus API에서 다음 상태를 확인한다.

- Application CPU와 Memory
- JVM Heap, GC
- DB CPU와 Memory
- HikariCP Active / Idle Connection과 Pending
- 필요 시 HTTP 요청 수와 JVM 관련 지표

Grafana는 사람이 확인할 수 있는 시각화 도구이며, 자동 판정의 유일한 입력으로 사용하지 않는다.

### 3. Tempo Trace

요청 내부의 어느 구간에서 시간이 소비되는지 확인한다.

- JDBC 구간
- 외부 HTTP I/O 구간
- Service 내부 로직 구간
- 전체 요청과 하위 Span의 관계

Trace가 충분히 세분화되어 있지 않다면 관측 보강 자체를 별도 실험으로 다룬다.
분석 중 임의로 계측 코드를 추가해 원래 성능 비교 조건을 바꾸지 않는다.

### 4. SQL / EXPLAIN ANALYZE

JDBC 구간이 길거나 SQL이 병목 후보인 경우 해당 Query와 다음 정보를 수집한다.

- 실제 실행 시간
- `COUNT` Query와 목록 Query의 차이
- 실행 계획과 Rows examined
- Index 사용 여부
- Sort, Temporary, Full Scan 등 실행 계획의 특징

SQL Evidence는 가능하면 `mysql-perf`에서 같은 Dataset과 같은 Query 조건으로 확인한다.

## 병목 진단 분기

### CPU 사용률이 높음

Prometheus에서 CPU 포화가 반복적으로 확인되고 요청 지연과 함께 움직이는지 확인한다.
필요한 경우에만 JFR을 추가 Evidence로 수집한다.

```text
CPU 병목 근거 확인
→ 선택적으로 JFR 실행
→ CPU Hot Method 확인
→ Trace / 코드 경로와 대조
```

JFR은 초기 Experiment의 필수 구성요소가 아니다.

### HikariCP Pending이 높음

다음 순서로 확인한다.

```text
HikariCP Pending 확인
→ 느린 SQL 확인
→ 긴 Transaction 확인
→ 실제 Pool 부족인지 확인
→ DB 처리시간과 Connection 점유시간 비교
```

Pending이 높다는 사실만으로 Connection Pool을 대규모로 늘리지 않는다.

### JDBC 시간이 큼

```text
Trace의 JDBC 구간 확인
→ SQL 수집
→ EXPLAIN ANALYZE
→ Query / Index / Fetch 구조 대안 비교
```

Index를 사용하지 않는다는 이유만으로 바로 Index를 추가하지 않는다.

### 외부 HTTP 시간이 큼

외부 호출의 응답시간, 호출 횟수, timeout 및 재시도 여부를 확인한다.
외부 시스템을 바꾸는 것은 초기 자동 개선 범위를 벗어날 수 있으므로 별도 실험으로 분리한다.

### JDBC와 외부 호출은 짧지만 전체 요청이 느림

Service 내부 로직, 직렬화, 반복 계산, 불필요한 변환 등 Trace에 드러나지 않은 구간을 확인한다.
필요한 계측을 추가해야 한다면 계측 Patch와 성능 개선 Patch를 구분해 기록한다.

## AI 개선 의사결정 규칙

AI는 다음 형식으로 분석을 남긴 뒤 Patch를 선택한다.

```text
목표 Metric 확인
→ k6 / Prometheus / Tempo / SQL Evidence 연결
→ 병목 후보와 근거 작성
→ 가능한 개선 대안 작성
→ 정확성·복잡도·영향 범위·Rollback 비용 비교
→ 가장 단순하고 영향 범위가 작은 대안 우선 선택
→ Patch
```

SQL 병목 후보라면 다음 대안을 함께 검토할 수 있다.

```text
A. Index 추가
B. Query Rewrite
C. Fetch / Join 구조 변경
D. 조회 구조 변경
E. Schema 변경
```

초기 자동 Patch의 기본 범위는 다음으로 제한한다.

- SQL
- Index
- 조회 Query
- 영향 범위가 작은 Backend 조회 로직

다음 항목은 초기 자동 개선 범위에서 제외하거나 별도 Experiment로 둔다.

- Connection Pool 대규모 변경
- Redis 도입
- Cache Architecture 변경
- Scale-out
- JVM 튜닝
- 인프라 구조 변경
- Schema 대규모 변경

## 재측정과 Gate

Patch 후에는 반드시 Test / Build / Validation을 먼저 통과시킨다.
그 다음 Docker Performance Environment와 Dataset을 다시 초기화하고, Patch에 포함된 의도한 Schema / Index 변경만 같은 순서로 적용한 뒤 Baseline과 동일한 조건으로 After를 실행한다.

```text
Baseline #1, #2, #3
→ 대표값 계산
→ Patch
→ After #1, #2, #3
→ 대표값 계산
→ Before / After 비교
```

단일 Run의 우연한 차이보다 반복 Run의 대표값과 개선 방향의 일관성을 우선한다.
최종 판정은 기능과 성능을 모두 확인한다.

```text
Functional Gate
- Test PASS
- Build PASS
- Validation PASS
- 기존 API 기능 정상

Performance Gate
- 목표 Metric이 Scenario 설정 최소 개선폭을 만족
- Error Rate가 허용 범위 이내
- Throughput의 심각한 감소 없음
- p99의 심각한 회귀 없음
- 반복 실행에서 같은 개선 방향 확인
```

Threshold는 Scenario별 설정으로 관리한다. 이 문서가 특정 숫자를 임의로 고정하지 않는다.

## 원칙

- Metric 하나만으로 원인을 확정하지 않는다.
- 운영 환경의 절대 RPS를 이 Harness의 성공 기준으로 사용하지 않는다.
- Before / After의 Dataset, Scenario, Build 환경·설정, Resource 조건을 바꾸지 않는다. Git SHA 차이는 의도한 Patch로 한정한다.
- 개선 후 동일 조건으로 다시 측정한다.
- 실패한 Experiment도 Report로 남겨 같은 시도를 반복하지 않도록 한다.
