# Automated Test Guide

이 프로젝트는 추가 테스트 패키지 없이 Python 표준 라이브러리 `unittest`로 API 테스트를 실행합니다. 테스트는 실제 `uvicorn` 서버를 `127.0.0.1:8765`에 잠깐 띄운 뒤 HTTP 요청을 보내 검증합니다.

## Run Tests

반드시 프로젝트의 `.venv` Python을 사용하세요.

```powershell
.\.venv\Scripts\python.exe -m unittest discover -s tests -v
```

## What It Covers

- Public API
  - `GET /api/v1/dramas`
  - `GET /api/v1/dramas/1`
  - `GET /api/v1/scenes/1`
  - `GET /api/v1/scenes/1/nearby-attractions`
- Excluded API
  - `GET /api/v1/dramas/search` returns `404`
  - `GET /api/v1/dramas/1/scenes` returns `404`
- Auth
  - login succeeds with the seed user
  - protected API returns `401` without token
  - protected API succeeds with `user-token`
- Mutations
  - wishlist add/list/delete
  - travel plan create/detail/update/delete
- Role Checks
  - USER token gets `403` on admin member list
  - ADMIN token can list and update members

## Notes

- Tests mutate the in-memory app state during the test process.
- Running the test command again starts a fresh Python process, so seed data is reset.
- Port `8765` must be free while the tests run.
