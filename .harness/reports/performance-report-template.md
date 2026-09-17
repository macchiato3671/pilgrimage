# Performance Experiment Report

상세 설정·Run별 원본·수집 근거는 실험별 파일로 보관하고 아래에서 링크한다.
공통 절차와 판정 규칙은 [README](../README.md)를 따른다.

## 1. 실험 조건

| 항목 | 값 또는 저장된 원본 링크 |
|---|---|
| Experiment ID / 측정 일시 | |
| 실험 Branch / Worktree | |
| Baseline / After Git SHA·Image Tag 또는 Digest | |
| Target URL / Scenario Version | |
| Dataset Version / Snapshot 식별 정보 | |
| Generator 버전 / Python 버전 / 생성 설정 | 코드 커밋, config_sha256, seed / profile / count / chunk |
| 실제 Row Count / 분포·무결성 검증 결과 | |
| 환경 설정 원본 | Host·Docker Resource, JVM·Spring·HikariCP, MySQL, Build·관측 설정 |
| 부하 설정 원본 | Case·입력값·비중, VU 또는 목표 RPS, 단계별 시간, 반복 횟수 |
| 대표값 집계 / Run 제외 규칙 | |
| 기준 환경·데이터 복원 확인 / 의도한 Schema·Index 변경 | |

## 2. 분석과 변경

- 목표 지표 / 병목 가설:
- 가설을 뒷받침하는 Metric·Trace·SQL 근거:
- 미확인 사항 또는 근거 간 불일치:
- Patch 요약 / Diff 링크:
- 초기 자동 수정 범위 포함 여부:

| 검토한 대안 | 기대 효과 | 영향 범위·위험·복구 비용 | 선택 이유 |
|---|---|---|---|
| | | | |

## 3. Before / After

| 지표 | Before 대표값 | After 대표값 | 변화율 |
|---|---:|---:|---:|
| RPS | | | |
| p50 | | | |
| p95 | | | |
| p99 | | | |
| Error Rate | | | |

- Case별 변화 / 반복 Run의 개선 방향 / 부작용:

| 원본·근거 | Before 링크 | After 링크 |
|---|---|---|
| Dataset manifest / fingerprint / verify.txt·추가 검증 | | |
| k6 Run별·Case별 결과 | | |
| Prometheus 측정 구간·결과 | | |
| Tempo 주요 Trace | | |
| 목록·COUNT SQL / 실행계획 | | |
| JFR (수집한 경우) | | |

## 4. 검증과 판정

| 검증 항목 | 적용 기준 | 결과·근거 |
|---|---|---|
| 기능 | Test / Build / Validation 통과, 기존 API 기능 정상 | |
| 목표 지표 | 지표·대상 Case·최소 개선폭: | |
| 오류율 | 허용 Threshold: | |
| 처리량 | 감소 허용폭: | |
| p99 | 회귀 허용폭: | |
| 반복 측정 | Run 수·집계·제외 규칙·개선 방향 일관성: | |

- 최종 결과 (`PASS` / `FAIL`) / 판정 사유:
- PASS 커밋 SHA 또는 FAIL 변경 폐기 여부:
- 후속 확인 사항:
- 사람의 Diff·Report 확인 / Merge 결정:
