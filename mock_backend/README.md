# Pilgrimage Mock API Server

FastAPI 기반 프론트 개발용 mock backend입니다. 모든 데이터는 메모리에 저장되며 서버를 재시작하면 초기 seed 상태로 돌아갑니다.

## 실행

새 환경에서 패키지 설치:

```powershell
.\.venv\Scripts\python.exe -m pip install -r requirements.txt
```

```powershell
.\.venv\Scripts\python.exe -m uvicorn main:app --reload --host 127.0.0.1 --port 8000
```

Swagger UI:

```text
http://127.0.0.1:8000/docs
```

수제 테스트용 PowerShell 시나리오는 `MANUAL_TESTS.md`를 참고하세요.

자동 테스트 실행 방법은 `TESTING.md`를 참고하세요.

## Auth Tokens

```text
Authorization: Bearer user-token
Authorization: Bearer admin-token
```

로그인 seed 계정:

```text
member@example.com / Password123!
admin@example.com / Admin123!
```

## API Coverage

구현: `API-001`, `API-002`, `API-005` through `API-024`

제외: `API-003`, `API-004`
