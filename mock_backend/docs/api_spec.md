# Pilgrimage Mock Backend API Spec

이 문서는 현재 Python mock backend 코드 기준으로 작성되었습니다.

## Base

- Base URL: `http://127.0.0.1:8000`
- Swagger UI: `/docs`
- Auth header: `Authorization: Bearer <token>`
- Seed tokens: `user-token`, `admin-token`
- Error shape: `{"detail": {"errorCode": "...", "message": "..."}}`

## Common Objects

### Member

```json
{
  "memberId": 1,
  "email": "member@example.com",
  "nickname": "여행자",
  "role": "USER",
  "status": "ACTIVE",
  "createdAt": "2026-06-03T10:00:00"
}
```

### Drama List Item

```json
{
  "id": 1,
  "title": "선재 업고 튀어",
  "genres": [{"genreId": 1, "name": "로맨스"}],
  "posterUrl": "https://picsum.photos/seed/drama-1/320/480",
  "releasedAt": "2024-01-01"
}
```

`releaseYear`는 제공하지 않습니다.

### Scene

```json
{
  "sceneId": 1,
  "dramaId": 1,
  "name": "솔이와 선재의 골목",
  "description": "주인공들이 처음 마음을 확인하는 골목길 장면입니다.",
  "address": "서울특별시 종로구 자하문로",
  "latitude": 37.5826,
  "longitude": 126.9706,
  "imgUrl": "https://picsum.photos/seed/scene-1/640/360"
}
```

### Place

```json
{
  "placeId": 1,
  "name": "경복궁",
  "description": "서울 도심의 대표 궁궐 관광지입니다.",
  "address": "서울특별시 종로구 사직로 161",
  "latitude": 37.5796,
  "longitude": 126.977,
  "imgUrl": "https://picsum.photos/seed/place-1/640/360",
  "contentId": 126508,
  "contentTypeId": 12,
  "contentTypeName": "관광지"
}
```

### Plan

```json
{
  "planId": 1,
  "memberId": 1,
  "title": "서울 드라마 여행",
  "beginDate": "2026-06-10",
  "endDate": "2026-06-12",
  "details": [
    {
      "detailId": 1,
      "dayNo": 1,
      "beginTime": "10:00",
      "endTime": "11:30",
      "sceneId": 1,
      "placeId": null
    }
  ],
  "createdAt": "2026-06-03T12:00:00",
  "updatedAt": "2026-06-03T12:00:00"
}
```

## Public APIs

### `GET /`

Mock server metadata를 반환합니다.

### `GET /api/v1/dramas`

드라마 목록을 조회합니다.

Query parameters:

- `keyword`: title 또는 description 부분 검색
- `OrderCondition`: `YEAR`, `GENRE`, `DESC`, `LATEST`

Response:

```json
{"dramas": ["Drama List Item"]}
```

### `GET /api/v1/dramas/search`

제외된 API입니다. 항상 `404 API_NOT_IMPLEMENTED`를 반환합니다.

### `GET /api/v1/dramas/{drama_id}`

드라마 상세와 해당 드라마의 씬 목록을 반환합니다.

Errors:

- `404 DRAMA_NOT_FOUND`

### `GET /api/v1/scenes/{scene_id}`

씬 상세를 반환합니다.

Errors:

- `404 SCENE_NOT_FOUND`

### `GET /api/v1/scenes/{scene_id}/nearby-attractions`

씬 주변 관광지를 반환합니다.

Query parameters:

- `radiusKm`: 기본값 `3`
- `contentTypeId`: 선택
- `page`: 기본값 `0`, 최소 `0`
- `size`: 기본값 `10`, 최소 `1`

Response fields:

- `sceneId`, `sceneName`, `sceneLatitude`, `sceneLongitude`
- `radiusKm`, `attractions`, `page`, `size`
- `totalElements`, `totalPages`, `hasNext`

Errors:

- `404 SCENE_NOT_FOUND`

### `GET /api/v1/places/{place_id}`

장소 상세를 반환합니다.

Errors:

- `404 PLACE_NOT_FOUND`

## Auth APIs

### `POST /api/v1/auth/login`

Request:

```json
{"email": "member@example.com", "password": "Password123!"}
```

Response:

```json
{
  "accessToken": "user-token",
  "refreshToken": "refresh-user-token",
  "tokenType": "Bearer",
  "expiresIn": 3600,
  "member": "Member"
}
```

Errors:

- `401 INVALID_CREDENTIALS`
- `403 MEMBER_ACCESS_DENIED`

### `POST /api/v1/members`

회원가입을 수행합니다.

Request:

```json
{"email": "new@example.com", "password": "Password123!", "nickname": "새사용자"}
```

Response: `Member`

Errors:

- `409 EMAIL_ALREADY_EXISTS`

## Member APIs

모든 API는 Bearer token이 필요합니다.

### `GET /api/v1/me`

현재 로그인한 회원 정보를 반환합니다.

### `PATCH /api/v1/me`

현재 로그인한 회원 정보를 수정합니다.

Request fields:

- `email`
- `nickname`
- `newPassword`

Response: `Member`

### `DELETE /api/v1/me`

현재 로그인한 회원을 탈퇴 처리합니다.

Request:

```json
{"password": "Password123!", "reason": "optional reason"}
```

Response:

```json
{"memberId": 1, "status": "WITHDRAWN", "deleted": true, "reason": "optional reason"}
```

## Wishlist APIs

모든 API는 Bearer token이 필요합니다.

### `POST /api/v1/wishlist/{scene_id}`

현재 회원의 위시리스트에 씬을 추가합니다. 이미 추가된 씬이면 기존 항목을 반환합니다.

Response:

```json
{
  "wishlistId": 1,
  "memberId": 1,
  "sceneId": 1,
  "createdAt": "2026-06-03T11:00:00",
  "scene": "Scene"
}
```

Errors:

- `404 SCENE_NOT_FOUND`

### `GET /api/v1/wishlist`

현재 회원의 위시리스트를 반환합니다.

Response:

```json
{
  "wishlists": [
    {
      "wishlistId": 1,
      "createdAt": "2026-06-03T11:00:00",
      "scene": "Scene"
    }
  ]
}
```

### `DELETE /api/v1/wishlist/{scene_id}`

현재 회원의 위시리스트에서 `sceneId` 기준으로 씬을 제거합니다.

Response:

```json
{"sceneId": 1, "deleted": true}
```

Errors:

- `404 SCENE_NOT_FOUND`
- `404 WISHLIST_NOT_FOUND`

## Travel Plan APIs

모든 API는 Bearer token이 필요합니다.

### `POST /api/v1/plans`

여행 계획을 생성합니다.

Request:

```json
{
  "title": "자동 테스트 일정",
  "beginDate": "2026-06-20",
  "endDate": "2026-06-21",
  "details": [
    {
      "dayNo": 1,
      "beginTime": "10:00",
      "endTime": "11:00",
      "sceneId": 1,
      "placeId": null
    }
  ]
}
```

Response: `Plan`

### `GET /api/v1/plans`

현재 회원의 여행 계획 목록을 반환합니다. 관리자는 전체 계획을 볼 수 있습니다.

Response:

```json
{"plans": ["Plan"]}
```

### `GET /api/v1/plans/{plan_id}`

여행 계획 상세를 반환합니다.

Errors:

- `403 TRAVEL_PLAN_ACCESS_DENIED`
- `404 TRAVEL_PLAN_NOT_FOUND`

### `PUT /api/v1/plans/{plan_id}`

여행 계획을 수정합니다. `details`는 새 detail id로 다시 생성됩니다.

Request: `POST /api/v1/plans`와 동일

Response: `Plan`

Errors:

- `403 TRAVEL_PLAN_ACCESS_DENIED`
- `404 TRAVEL_PLAN_NOT_FOUND`

### `DELETE /api/v1/plans/{plan_id}`

여행 계획을 삭제합니다.

Response:

```json
{"planId": 1, "deleted": true}
```

Errors:

- `403 TRAVEL_PLAN_ACCESS_DENIED`
- `404 TRAVEL_PLAN_NOT_FOUND`

## Admin Member APIs

모든 API는 admin Bearer token이 필요합니다.

Admin 권한이 없으면 `403 ADMIN_AUTHORITY_REQUIRED`를 반환합니다.

### `GET /api/v1/members`

전체 회원 목록을 반환합니다.

Response:

```json
{"members": ["Member"]}
```

### `GET /api/v1/members/{member_id}`

회원 상세를 반환합니다.

Errors:

- `404 MEMBER_NOT_FOUND`

### `PATCH /api/v1/members/{member_id}`

회원 정보를 수정합니다.

Request fields:

- `email`
- `nickname`
- `role`: `USER` 또는 `ADMIN`
- `status`
- `newPassword`

Response: `Member`

Errors:

- `404 MEMBER_NOT_FOUND`

### `DELETE /api/v1/members/{member_id}`

회원을 탈퇴 처리합니다.

Response:

```json
{"memberId": 1, "status": "WITHDRAWN", "deleted": true}
```

Errors:

- `400 CANNOT_DELETE_SELF`
- `404 MEMBER_NOT_FOUND`
