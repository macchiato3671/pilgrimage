# Performance Experiment Report

## 1. Experiment

- Experiment ID:
- Target: local Docker Performance Environment
- Target Base URL:
- Scenario:
- Dataset Version:
- Dataset Row Count:
- Baseline Git SHA:
- After Git SHA:
- Experiment Branch:
- Baseline Application Image / Tag:
- After Application Image / Tag:
- Docker Resource Profile:
- JVM / Application / HikariCP 설정:
- MySQL Version / 주요 설정:
- k6 Scenario Version:
- Load Model / VU / 목표 RPS:
- Duration / Warm-up / Cool-down:
- Case Mix:
- 대표값 집계 방식:

## 2. Baseline

### Run Summary

| Run | RPS | p50 | p95 | p99 | Error Rate | 비고 |
|---|---:|---:|---:|---:|---:|---|
| Baseline #1 | | | | | | |
| Baseline #2 | | | | | | |
| Baseline #3 | | | | | | |
| 대표값 | | | | | | |

### Baseline Evidence

- k6 Case별 결과:
- Prometheus CPU / Memory / JVM / HikariCP:
- Tempo 주요 Trace:
- 주요 SQL / `EXPLAIN ANALYZE`:
- 선택적 JFR 결과:

## 3. Analysis

- 목표 Metric:
- 관측된 병목 후보:
- 병목 가설:
- 가설의 근거가 되는 k6 / Metric / Trace / SQL Evidence:
- Evidence 사이의 불일치 또는 미확인 사항:

### 검토한 대안

| 대안 | 기대 효과 | 영향 범위 | Trade-off / 위험 | 선택 여부 |
|---|---|---|---|---|
| | | | | |

- 선택한 개선안:
- 선택 이유:

## 4. Patch 및 Validation

- Patch 요약:
- Changed Files:
- 변경 범위가 초기 자동 개선 정책에 포함되는지:
- Test Result:
- Build Result:
- Validation Result:
- 기능 회귀 확인:

## 5. After

### Run Summary

| Run | RPS | p50 | p95 | p99 | Error Rate | 비고 |
|---|---:|---:|---:|---:|---:|---|
| After #1 | | | | | | |
| After #2 | | | | | | |
| After #3 | | | | | | |
| 대표값 | | | | | | |

### After Evidence

- k6 Case별 결과:
- Prometheus CPU / Memory / JVM / HikariCP:
- Tempo 주요 Trace:
- 주요 SQL / `EXPLAIN ANALYZE`:
- 선택적 JFR 결과:

## 6. Before / After 비교

| Metric | Before 대표값 | After 대표값 | 변화율 | 판정 근거 |
|---|---:|---:|---:|---|
| RPS | | | | |
| p50 | | | | |
| p95 | | | | |
| p99 | | | | |
| Error Rate | | | | |

- 주요 변화:
- Case별 변화:
- 부작용 / Regression:
- 반복 실행에서 개선 방향이 일관적인지:
- 추가 확인 사항:

## 7. Gate

### Functional Gate

- Test PASS:
- Build PASS:
- Validation PASS:
- 기존 API 기능 정상:

### Performance Gate

- 목표 Metric:
- Scenario별 최소 개선 Threshold:
- Error Rate 허용 Threshold:
- Throughput degradation 허용 Threshold:
- p99 regression 허용 Threshold:
- 반복 실행 조건 만족:
- Gate Result: `PASS` / `FAIL`

Threshold는 Scenario 설정값을 참조하며, Experiment마다 적용된 값을 이 Report에 기록한다.

## 8. Disposition

- Result: `PASS` / `FAIL`
- 실패 사유 또는 보류 사유:
- Commit SHA: PASS일 때만 기록
- FAIL 시 변경사항 폐기 여부:
- 사람의 diff / Report 확인:
- Merge 결정: 사람이 결정
