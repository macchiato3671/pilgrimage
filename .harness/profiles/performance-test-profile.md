# Performance Test Profile

## 1. 목적

성능 개선 전후의 결과를 동일한 조건에서 비교하기 위해 로컬 격리 성능 실험 환경의 고정 조건을 정의한다.
이 Profile은 운영 서버의 절대 최대 성능이나 실제 사용자 네트워크를 재현하기 위한 문서가 아니다.

비교 대상은 다음과 같다.

```text
같은 로컬 환경의 변경 전 성능
vs
같은 로컬 환경의 변경 후 성능
```

## 2. 실행 위치와 구성

모든 구성요소는 로컬 Host와 로컬 Docker Network 안에서 실행한다.

```text
Local Host
├─ Orchestrator
├─ AI Agent / Codex
├─ k6
├─ Git / Worktree
└─ Report 저장소

Docker Performance Environment
├─ nginx
├─ backend
├─ mysql-perf
├─ prometheus
├─ tempo
└─ grafana
```

- Load Generator: 로컬 Host의 k6
- Target Base URL: 기본값 `http://localhost`, 실제 값은 Experiment 설정에 기록
- Nginx: 로컬 Docker Performance Environment의 Gateway
- Application: Spring Boot Docker Container, 초기 Profile은 1 Instance
- Database: Performance 전용 MySQL 8.4 Container인 `mysql-perf`
- Prometheus: Metric 수집 및 API 조회 대상
- Tempo: Trace 수집 및 API 조회 대상
- Grafana: 사람이 관찰하기 위한 시각화 도구

요청 경로는 다음과 같다.

```text
k6
→ local nginx
→ backend
→ mysql-perf
```

Harness 자동 분석은 Grafana 화면에 의존하지 않고 Prometheus API, Tempo API, k6 결과,
MySQL / EXPLAIN ANALYZE 결과를 직접 사용한다.

## 3. Git 실험 격리

개발자의 현재 작업을 AI 실험이 오염시키지 않도록 별도의 Worktree와 branch를 사용한다.

```text
pilgrimage/
└─ 현재 개발 작업

pilgrimage-perf-worktree/
└─ Experiment 전용 작업 영역
```

예시 branch는 `perf/experiment-001`이다.

- Harness와 AI Agent는 Experiment 전용 영역에서만 코드를 수정한다.
- Experiment 시작 시 기준 Git SHA와 실험 branch를 기록한다.
- PASS일 때만 실험 branch에 commit을 생성한다.
- FAIL일 때는 commit하지 않고 실험 변경을 폐기한다.
- 자동 merge는 수행하지 않으며, 사람이 diff와 Report를 확인한 뒤 merge한다.

## 4. 고정해야 하는 환경 조건

Before / After 사이에 다음 조건을 고정하고 Report에 기록한다.

- Docker CPU / Memory 및 기타 Resource 제한
- Docker Image Tag / Digest와 Application Git SHA를 각 Run에 기록한다. Before / After의 SHA 차이는 의도한 Patch로 한정한다.
- Application Instance 수
- JVM Option
- Spring Application 설정
- HikariCP 설정
- MySQL Version 및 주요 DB 설정
- Performance Dataset Version과 Row 수
- k6 VU 또는 목표 RPS 기반 Load Model, Scenario, Case 비중, 입력값, Duration
- Warm-up과 Cool-down 시간

테스트 중 Docker Build, 대용량 파일 복사, 다른 고부하 작업을 함께 수행하지 않는다.
하나의 Experiment에서는 가능한 한 하나의 주요 변수만 변경한다.

## 5. Performance Dataset

- Dataset: `perf-dataset-v1`
- Database: `mysql-perf`
- Dataset 정의: [`perf-dataset-v1.md`](../datasets/perf-dataset-v1.md)
- 개발용 DB, 기존 개인 DB, 실제 배포 환경의 DB를 사용하지 않는다.

각 Experiment는 가능하면 다음 순서로 시작한다.

```text
Docker Performance Environment 초기화
→ mysql-perf 초기화
→ Schema 적용
→ 고정 Dataset 적재
→ Row 수·분포·NULL 비율 검증
→ Warm-up
→ Test
```

Before / After에서 다음 조건은 동일해야 한다.

- Dataset Version
- Row 수와 데이터 분포
- 검색 키워드 분포
- Content Type 분포
- 좌표와 Hotspot 분포
- NULL 비율
- 검색 결과 규모
- Pagination 대상 규모

Schema, Index, Data 상태가 바뀌는 Experiment라면 After 측정 직전에 기준 Dataset과 환경을 다시 초기화한다.
그 후 Patch에 포함된 의도한 Schema / Index 변경만 적용하고, 데이터 자체는 같은 Version으로 다시 적재한다.
기준 상태로 복구할 수 없거나 Patch 외의 상태가 함께 바뀐 실험은 동일한 Before / After 비교로 판정하지 않는다.

## 6. Test Rules

- 동일한 Dataset과 동일한 k6 Scenario를 사용한다.
- 동일한 Application Instance 수와 Application 설정을 사용한다.
- 동일한 MySQL 설정과 Docker Resource 조건을 사용한다.
- Warm-up 요청은 Baseline / After 통계에서 분리한다.
- Baseline과 After는 가능하면 각각 3회 이상 반복한다.
- 대표값 집계 방식과 제외 규칙은 Scenario 설정에 기록한다.
- Capacity 탐색이나 부하 한계 확인은 Before / After Gate와 분리한다.

이 Harness의 성공 기준은 운영 환경의 절대 RPS가 아니라 같은 로컬 조건에서의 개선 여부다.

## 7. Warm-up / Measurement

Place Search Scenario의 초기 고정 조건은 다음과 같다.

| 단계 | 시간 | 부하 |
|---|---:|---|
| Warm-up | 1분 | 1 VU |
| Measurement | 3분 | 10 VU |
| Cool-down | 1분 | 1 VU |

Case Mix는 다음과 같다.

- PS-01: 30%
- PS-02: 25%
- PS-03: 25%
- PS-04: 10%
- PS-05: 10%

Baseline과 After의 반복 횟수, VU, Duration, Case 비중, 요청 입력값은 동일하게 유지한다.
초기 고정값을 변경할 필요가 있으면 Scenario Version 또는 별도 Experiment 설정으로 기록한다.

## 8. Evidence 수집 조건

같은 측정 구간에 다음 Evidence를 수집한다.

- k6: Case별 응답시간, RPS, Error Rate, p95, p99
- Prometheus: CPU, JVM, Memory, HikariCP, DB 상태
- Tempo: 전체 요청, JDBC, 외부 HTTP, 내부 Service 구간
- MySQL: Query 실행시간, Slow Query, `EXPLAIN ANALYZE`
- JFR: Prometheus에서 CPU 병목이 확인된 경우에만 선택적으로 수집

JFR은 초기 Profile의 필수 구성요소가 아니다.
