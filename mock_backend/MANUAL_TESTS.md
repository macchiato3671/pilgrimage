# Manual Test Guide

서버 실행:

```powershell
.\.venv\Scripts\python.exe -m uvicorn main:app --reload --host 127.0.0.1 --port 8000
```

Base URL:

```text
http://127.0.0.1:8000
```

## 1. Public APIs

```powershell
Invoke-RestMethod http://127.0.0.1:8000/api/v1/dramas
Invoke-RestMethod http://127.0.0.1:8000/api/v1/dramas/1
Invoke-RestMethod http://127.0.0.1:8000/api/v1/scenes/1
Invoke-RestMethod http://127.0.0.1:8000/api/v1/places/1
Invoke-RestMethod "http://127.0.0.1:8000/api/v1/scenes/1/nearby-attractions?radiusKm=3&page=0&size=10"
```

제외 API는 404가 나와야 합니다.

```powershell
Invoke-WebRequest http://127.0.0.1:8000/api/v1/dramas/search
Invoke-WebRequest http://127.0.0.1:8000/api/v1/dramas/1/scenes
```

## 2. Login And Auth

```powershell
$login = Invoke-RestMethod `
  -Method Post `
  -Uri http://127.0.0.1:8000/api/v1/auth/login `
  -ContentType "application/json" `
  -Body (@{ email="member@example.com"; password="Password123!" } | ConvertTo-Json)

$userHeaders = @{ Authorization = "Bearer $($login.accessToken)" }
$adminHeaders = @{ Authorization = "Bearer admin-token" }
```

토큰 없이 호출하면 401이 나와야 합니다.

```powershell
Invoke-WebRequest http://127.0.0.1:8000/api/v1/me
```

토큰 포함 호출은 성공해야 합니다.

```powershell
Invoke-RestMethod -Headers $userHeaders http://127.0.0.1:8000/api/v1/me
```

## 3. Wishlist Flow

```powershell
Invoke-RestMethod -Method Post -Headers $userHeaders http://127.0.0.1:8000/api/v1/wishlist/2
Invoke-RestMethod -Headers $userHeaders http://127.0.0.1:8000/api/v1/wishlist
Invoke-RestMethod -Method Delete -Headers $userHeaders http://127.0.0.1:8000/api/v1/wishlist/1
Invoke-RestMethod -Headers $userHeaders http://127.0.0.1:8000/api/v1/wishlist
```

## 4. Travel Plan Flow

```powershell
$planBody = @{
  title = "서울 드라마 여행 테스트"
  beginDate = "2026-06-20"
  endDate = "2026-06-21"
  details = @(
    @{
      dayNo = 1
      beginTime = "10:00"
      endTime = "11:30"
      sceneId = 1
      placeId = $null
    }
  )
} | ConvertTo-Json -Depth 5

$plan = Invoke-RestMethod `
  -Method Post `
  -Headers $userHeaders `
  -Uri http://127.0.0.1:8000/api/v1/plans `
  -ContentType "application/json" `
  -Body $planBody

Invoke-RestMethod -Headers $userHeaders http://127.0.0.1:8000/api/v1/plans
Invoke-RestMethod -Headers $userHeaders "http://127.0.0.1:8000/api/v1/plans/$($plan.planId)"

$updateBody = @{
  title = "수정된 서울 드라마 여행"
  beginDate = "2026-06-22"
  endDate = "2026-06-23"
  details = @()
} | ConvertTo-Json -Depth 5

Invoke-RestMethod `
  -Method Put `
  -Headers $userHeaders `
  -Uri "http://127.0.0.1:8000/api/v1/plans/$($plan.planId)" `
  -ContentType "application/json" `
  -Body $updateBody

Invoke-RestMethod -Method Delete -Headers $userHeaders "http://127.0.0.1:8000/api/v1/plans/$($plan.planId)"
```

## 5. Member Flow

```powershell
$signupBody = @{
  email = "new-member@example.com"
  password = "Password123!"
  nickname = "새회원"
} | ConvertTo-Json

Invoke-RestMethod `
  -Method Post `
  -Uri http://127.0.0.1:8000/api/v1/members `
  -ContentType "application/json" `
  -Body $signupBody

$meUpdateBody = @{
  nickname = "수정된닉네임"
  newPassword = "NewPassword123!"
} | ConvertTo-Json

Invoke-RestMethod `
  -Method Patch `
  -Headers $userHeaders `
  -Uri http://127.0.0.1:8000/api/v1/me `
  -ContentType "application/json" `
  -Body $meUpdateBody
```

## 6. Admin Flow

USER 토큰은 관리자 API에서 403이 나와야 합니다.

```powershell
Invoke-WebRequest -Headers $userHeaders http://127.0.0.1:8000/api/v1/members
```

ADMIN 토큰은 성공해야 합니다.

```powershell
Invoke-RestMethod -Headers $adminHeaders http://127.0.0.1:8000/api/v1/members
Invoke-RestMethod -Headers $adminHeaders http://127.0.0.1:8000/api/v1/members/1
```

관리자 회원 수정:

```powershell
$adminUpdateBody = @{
  nickname = "관리자가수정"
  status = "ACTIVE"
  role = "USER"
} | ConvertTo-Json

Invoke-RestMethod `
  -Method Patch `
  -Headers $adminHeaders `
  -Uri http://127.0.0.1:8000/api/v1/members/1 `
  -ContentType "application/json" `
  -Body $adminUpdateBody
```

## 7. Reset State

모든 데이터는 메모리에만 있습니다. 테스트 중 꼬이면 서버 프로세스를 종료하고 다시 실행하면 초기 seed 데이터로 돌아갑니다.
