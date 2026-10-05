# Pilgrimage Performance Harness

동일하게 초기화한 로컬 환경에서 변경 전후를 반복 측정해 AI의 성능 개선 효과를 검증한다.
운영 서버의 최대 처리량을 추정하는 용도로 사용하지 않는다.
실험(Experiment)은 코드·환경·데이터·시나리오·측정 근거·검증 결과를 하나로 묶은 기록이다.

## 문서 구성

| 문서 | 정의하는 내용 |
|---|---|
| [환경](profiles/performance-test-profile.md) | 구성요소와 고정할 실행 조건 |
| [데이터](datasets/perf-dataset-v1.md) | 생성 출처, 규모·분포, 적재·검증 계약 |
| [시나리오](scenarios/place-search.md) | 요청값·부하, 정확성 검증, 성능 판정 설정 |
| [보고서](reports/performance-report-template.md) | 실험 조건, 변경 근거, 비교 결과와 판정 |

이 문서는 실행 계약이다. k6 스크립트와 Orchestrator 구현은 별도 작업으로 다룬다.
데이터 준비는 저장소의 Mock Generator를 사용한다. [로컬 실행 절차](datasets/perf-dataset-v1.md#로컬-실행-예시)에 따라 적재·검증 후 측정을 시작한다.

## 실험 범위

- 전용 worktree·브랜치와 로컬 Docker 환경에서 실험한다. 현재 개발 작업 영역은 수정하지 않는다.
- 데이터는 성능 전용 `mysql-perf`에 적재한다. 개발·운영 DB는 사용하지 않는다.
- 초기 AI 수정 범위는 SQL, 인덱스, 조회 Query와 영향 범위가 작은 Backend 조회 로직이다.
- Pool 대규모 변경, Redis·캐시 구조 도입, Scale-out, JVM 튜닝, 인프라·Schema 대규모 변경은 별도 실험으로 분리한다.
- 계측 보강이 필요하면 별도 실험으로 분리해 Before / After의 관측 조건을 맞춘다.

## 실험 절차

1. 실험 ID, 기준 Git SHA, 전용 worktree·브랜치와 실행 설정을 기록한다.
2. Docker 환경과 DB를 초기화하고 Schema·고정 Dataset을 적재한 뒤 무결성·분포를 검증한다.
3. 워밍업 후 Baseline을 반복 측정하고 같은 측정 구간의 근거를 수집한다.
4. 병목 가설과 개선 대안을 비교해 정확성·복잡도·영향 범위·복구 비용을 검토한다.
5. 가장 단순하고 영향 범위가 작은 개선안을 적용하고 Test / Build / Validation을 수행한다.
6. 기준 환경·데이터를 복원하고 의도한 Schema / Index 변경만 적용한다.
7. 동일한 워밍업·시나리오로 After를 반복 측정하고 대표값과 Case별 결과를 비교한다.
8. 기능·성능 Gate를 판정하고 성공·실패 모두 보고서로 남긴다.

## 비교 규칙

- Before / After의 Dataset, Scenario, Build 환경·설정, Docker Resource 조건을 고정한다.
- Git SHA와 이미지의 차이는 의도한 Patch로 한정하고, 한 실험에서는 가능한 한 하나의 주요 변수만 바꾼다.
- 데이터를 다시 적재해도 동일한 데이터가 유지되어야 한다. 재현 기준은 Dataset 계약에 따른다.
- 워밍업 결과는 측정 통계에서 제외한다. Baseline과 After는 가능하면 각각 3회 이상 반복한다.
- 반복 횟수, 대표값 계산 방식, Run 제외 규칙은 측정 전에 확정하고 양쪽에 동일하게 적용한다.
- Capacity 탐색 결과는 비교 측정에 섞지 않는다. 설정이나 데이터가 달라진 결과는 동일 조건 비교로 판정하지 않는다.

## 분석 근거

| 입력 | 확인할 내용 |
|---|---|
| k6 | 전체·Case별 응답시간(p50/p95/p99), RPS, 요청 수, 오류율·HTTP Status |
| Prometheus API | Application·DB CPU/Memory, JVM Heap·GC, HikariCP Active/Idle/Pending |
| Tempo API | 전체 요청과 JDBC·외부 HTTP·Service 구간의 시간 |
| MySQL | 목록·COUNT Query의 실행시간, Slow Query, `EXPLAIN ANALYZE`, 인덱스·스캔·정렬 |

단일 지표로 원인을 확정하지 않고 Metric·Trace·SQL을 연결해 가설을 세운다.
SQL 근거는 같은 Dataset과 요청 조건의 `mysql-perf`에서 확인한다.
Grafana는 사람이 탐색하는 도구이며 자동 분석은 API와 원본 결과를 사용한다.
JFR은 CPU 병목 근거가 있을 때만 선택적으로 수집한다.

## 판정과 결과 처리

| Gate | 통과 조건 |
|---|---|
| 기능 | Test / Build / Validation 통과, 기존 API 기능·검색 의미 유지 |
| 성능 | 목표 지표의 최소 개선폭 충족, 오류율·처리량·p99 회귀 허용 범위 준수, 반복 실행에서 개선 방향 일관 |

구체적인 Threshold와 집계 방식은 시나리오 설정에 두고 보고서에 적용값을 기록한다.
미확정 설정은 측정 전에 확정하며, 기준 없이 PASS를 판정하지 않는다.

- PASS: 실험 브랜치에 커밋하고 SHA와 보고서를 남긴다.
- FAIL: 보고서와 근거를 보존하고 커밋하지 않은 실험 변경을 폐기한다.
- 자동 merge는 하지 않는다. 사람이 diff와 보고서를 확인해 merge 여부를 결정한다.
