# Pilgrimage Performance Harness

## 목적

Pilgrimage Performance Harness는 운영 서버의 절대적인 최대 성능을 측정하기 위한 도구가 아니다.
동일하게 초기화된 로컬 성능 실험 환경에서 변경 전과 변경 후를 반복 측정하여,
AI가 제안한 성능 개선이 실제로 효과가 있는지 검증하는 것이 목적이다.

Harness의 기본 단위는 하나의 `Experiment`다. Experiment는 고정된 Git 실험 영역,
Docker 성능 환경, Dataset, k6 Scenario, Evidence, Validation 결과를 하나의 기록으로 묶는다.

```text
Harness 실행
→ Git 실험 영역 준비
→ 로컬 Docker 성능 환경 초기화
→ 고정 Dataset 적재 및 검증
→ Warm-up
→ Baseline k6 실행
→ Metric / Trace / SQL Evidence 수집
→ AI 병목 분석
→ 개선 대안 및 Trade-off 검토
→ AI Patch
→ Test / Build / Validation
→ Docker / Dataset 재초기화
→ 동일 Scenario 실행
→ Before / After 비교
→ Functional Gate / Performance Gate
→ PASS: 실험 branch에 commit 및 Report 생성
→ FAIL: commit하지 않고 실험 변경 폐기
```

자동 merge는 수행하지 않는다. PASS 이후에도 사람의 diff와 Report 확인을 거쳐 사람이 merge 여부를 결정한다.

## 실행 환경

```text
Local Host
├─ Orchestrator
├─ AI Agent / Codex
├─ k6
├─ Git / Worktree
└─ Performance Report

Docker Performance Environment
├─ nginx
├─ backend
├─ mysql-perf
├─ prometheus
├─ tempo
└─ grafana
```

측정 대상은 Local Host 안에서 실행되는 격리된 Docker Performance Environment다.
개발용 DB나 실제 배포 환경의 데이터를 측정 대상으로 사용하지 않는다.

Grafana는 사람이 상태를 관찰하고 Trace·Metric을 탐색하기 위한 도구다.
Harness와 AI 분석은 가능하면 다음 API와 결과를 직접 사용한다.

```text
Prometheus API
Tempo API
k6 결과
MySQL / EXPLAIN ANALYZE
```

## Git 실험 격리

현재 개발 작업과 AI 실험 변경을 분리한다.

```text
pilgrimage/
└─ 현재 개발 작업

pilgrimage-perf-worktree/
└─ Performance Harness 전용 실험
```

Experiment마다 `perf/experiment-001`과 같은 별도 branch를 사용할 수 있다.
Harness와 AI Agent는 실험 영역에서만 코드를 수정한다.

- PASS: 실험 branch에 commit을 생성하고 Report에 Commit SHA를 기록한다.
- FAIL: commit하지 않고 변경을 폐기한다.
- 어느 경우에도 현재 개발 작업 영역을 수정하거나 자동 merge하지 않는다.

## 문서 구조

```text
.harness/
├─ README.md
├─ analysis-flow.md
├─ datasets/
│  └─ perf-dataset-v1.md
├─ profiles/
│  └─ performance-test-profile.md
├─ scenarios/
│  └─ place-search.md
└─ reports/
   └─ performance-report-template.md
```

- `analysis-flow.md`: 실험 시작부터 Gate와 commit/discard까지의 분석·자동화 흐름
- `datasets/`: 고정 Dataset의 생성 규칙, 분포, 초기화·검증 조건
- `profiles/`: 로컬 Docker 성능 환경과 실험에서 고정해야 하는 조건
- `scenarios/`: 성능 테스트 대상과 고정 요청 조건
- `reports/`: 성공·실패 Experiment의 Before / After와 Evidence 기록 양식

k6 실행 스크립트와 Orchestrator 코드는 현재 문서 작업 범위에 포함하지 않는다.
Scenario 문서는 향후 구현될 실행 스크립트가 따라야 할 계약을 정의한다.

## 공통 원칙

- Before / After는 같은 Dataset Version, Scenario, Build 환경·설정, Docker Resource 조건으로 실행하고, Git SHA 차이는 의도한 Patch로 한정한다.
- Dataset은 매 Experiment 전에 `mysql-perf`에 다시 적재하고, Row 수와 분포를 검증한다.
- Warm-up 결과는 측정 통계에서 분리한다.
- Baseline과 After는 가능하면 여러 번 실행하고 Scenario에 정의된 대표값으로 비교한다.
- Metric 하나만으로 병목을 확정하지 않고 Metric, Trace, SQL Evidence를 연결해 가설을 만든다.
- AI는 가장 단순하고 영향 범위가 작은 개선안을 우선 검토한다.
- Functional Gate와 Performance Gate를 모두 통과해야 PASS다.
- Gate Threshold는 Scenario별 설정값으로 관리하며 이 README에서 임의로 확정하지 않는다.
