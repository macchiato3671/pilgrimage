# Performance Test Profile

로컬 성능 실험의 환경 구성과 고정 조건을 정의한다.
실험 절차·반복 측정·판정 규칙은 [공통 계약](../README.md)을 따른다.

## 구성

| 위치 | 구성요소 | 역할 |
|---|---|---|
| Local Host | Orchestrator, AI Agent / Codex | 실험 실행·분석·Patch |
| Local Host | k6, Git / Worktree, Report 저장소 | 부하 생성·변경 격리·결과 기록 |
| Docker | nginx | 요청 Gateway |
| Docker | Spring Boot backend | 측정 대상, 초기 1 Instance |
| Docker | MySQL 8.4 `mysql-perf` | 성능 실험 전용 DB |
| Docker | prometheus, tempo, grafana | Metric·Trace 수집과 탐색 |

```text
k6 (Local Host) → nginx → backend → mysql-perf
```

Target Base URL 기본값은 `http://localhost`(80)이다. `PERF_HTTP_PORT`를 변경하면 실제 주소를 실험 설정에 기록한다.
모든 측정 대상은 로컬 Docker Network 안에서 실행한다.

## Before / After 고정 조건

| 구분 | 기록하고 고정할 값 |
|---|---|
| Host / Docker | 실행 장비 정보, CPU·Memory 및 기타 Resource 제한 |
| Build | 빌드 환경·설정, 각 Run의 Application Git SHA와 Image Tag / Digest |
| Application | Instance 수, JVM Option, Spring 설정, HikariCP 설정 |
| Database | MySQL 버전과 주요 DB 설정 |
| Dataset | Version, Snapshot 식별 정보, 실제 Row 수와 분포 검증 결과 |
| Scenario | Version, Target URL, Load Model, 입력값·Case 비중, VU 또는 목표 RPS |
| 측정 | 반복 횟수, Warm-up / Measurement / Cool-down 시간, 집계·제외 규칙 |
| 관측 | Metric·Trace·SQL 수집 설정과 측정 구간 |

Git SHA·이미지의 차이는 의도한 Patch로 한정한다.
테스트 중 Docker Build, 대용량 파일 복사 등 다른 고부하 작업을 함께 수행하지 않는다.
설정 원본은 실험별로 보관하고 [보고서](../reports/performance-report-template.md)에서 참조한다.

## 데이터 준비

[perf-dataset-v1](../datasets/perf-dataset-v1.md)의 초기화·적재·검증 계약을 따른다.
Host의 Python·MySQL CLI로 `run --dry-run → run → verify`를 수행한다. 생성 기준과 명령은 Dataset 문서에 둔다.
접속 대상은 `mysql-perf`의 호스트 공개 주소이며 성능 Compose 기본값은 `127.0.0.1:3306/moonbackdb`다.
개발 DB 또는 이전 실험의 잔존 Volume에 이어서 적재하지 않는다.
After에서는 기준 환경과 데이터를 복원하고 Patch에 포함된 Schema / Index 변경만 적용한다.

## 부하와 관측

- 요청값, Case 비중, 단계별 시간·VU는 [Place Search 시나리오](../scenarios/place-search.md)에만 정의한다.
- 수집 지표와 분석 원칙은 [공통 계약](../README.md#분석-근거)을 따른다.
- 필요한 지표·Span이 수집되는지 측정 전에 확인한다. 관측 보강은 성능 Patch와 분리한다.
