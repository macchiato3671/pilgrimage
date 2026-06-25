# 필그리미지 Frontend

좋아한 드라마·영화의 촬영지를 주변 여행 장소 및 일정으로 연결하는 Vue 3 데스크톱 웹 앱입니다. 제공된 와이어프레임을 한국어 UI로 재구성했습니다.

## 기술 스택

- Vite
- Vue.js 3 (`<script setup>`)
- JavaScript
- Pinia
- Vue Router
- Axios
- Kakao Maps JavaScript SDK
- 브라우저 기본 HTML5 Drag & Drop

## 실행

```bash
pnpm install
pnpm dev
```

기본 프론트엔드 주소는 `http://localhost:5173`, 기본 백엔드 주소는 `http://localhost:8080/api/v1`입니다.

프로덕션 빌드 검증:

```bash
pnpm build
pnpm preview
```

## 환경 변수

`.env.example`을 참고합니다.

```dotenv
VITE_API_BASE_URL=http://localhost:8080/api/v1
VITE_KAKAO_MAP_APP_KEY=
VITE_TEST_EMAIL=member@example.com
VITE_TEST_PASSWORD=Password123!
```

`VITE_KAKAO_MAP_APP_KEY`에는 Kakao Developers의 JavaScript 키를 입력합니다. 개발 서버를 사용할 때는 `http://localhost:5173`을 해당 키의 JavaScript SDK 도메인에 등록해야 합니다.

테스트 계정 변수는 로그인 화면의 **테스트 계정 채우기** 버튼에서만 사용하며 자동 로그인하지 않습니다.

## 구현 기능

### 작품 및 촬영지

- 연도 목차 및 연도별 작품 목록
- 장르 목차 및 장르별 작품 목록
- 키워드 작품 검색
- 작품별 촬영지 목록과 지도 마커
- 촬영지 상세 이미지, 설명, 주소, 좌표, 방문 팁
- 촬영지 기준 주변 장소 이동

### 주변 장소

- 키워드·카테고리·현재 지도 중심 기반 장소 검색
- 음식점, 카페, 관광지, 문화시설, 숙박, 쇼핑, 레포츠 필터
- 촬영지 주변 관광지 조회
- 장소 상세 정보와 카카오맵 링크

### 위시리스트

- 로그인 사용자는 서버 API 사용
- 비회원은 `localStorage` 사용
- 작품별 그룹 필터
- 모든 지도에서 위시리스트 촬영지 마커 상시 표시
- 촬영지 추가·제거의 낙관적 UI 처리

### 여행 계획

- 로그인 사용자는 서버 API, 비회원은 `localStorage` 사용
- 계획 생성·조회·수정·삭제
- 프론트엔드 전용 핀 색상 관리
- 날짜별 일정 조회
- 전역 플로팅 일정 편집기
- 촬영지·장소 카드 Drag & Drop 추가
- 날짜 간 일정 이동, 시간 변경, 삭제
- 편집기 최소화 및 페이지 간 편집 상태 유지
- 세부 일정 일괄 동기화 API 사용

### 인증과 로컬 동기화

- 로그인 및 회원가입
- 테스트 계정 빠른 입력
- JWT Authorization 헤더 자동 적용
- 401 응답 시 세션 정리 및 로그인 이동
- 로그인 전에 생성한 위시리스트·계획이 있으면 `/sync`로 라우팅
- 동기화 진행률과 항목별 처리 로그 표시
- 계획 생성 후 세부 일정까지 순차 등록
- 성공한 로컬 항목을 즉시 제거하여 재시도 시 중복 최소화
- 전체 성공 또는 사용자의 폐기 선택 후 로컬 작업 삭제

## MVVM 구조

```text
src/
├── api/          # Model/Infrastructure: Axios, Kakao SDK 로더, API 서비스
├── models/       # Model: DTO 정규화, 날짜·계획·저장소 규칙
├── stores/       # ViewModel: 화면 상태와 유스케이스를 담당하는 Pinia Store
├── views/        # View: Router 단위 페이지
├── components/   # View: 재사용 UI, 지도, 카드, 일정 편집기
├── router/       # 인증·로컬 동기화 라우팅 정책
└── config/       # 색상, 장소 분류, 저장소 키 등 변경 지점
```

View는 API나 `localStorage`를 직접 변경하지 않고 Pinia ViewModel의 액션을 호출합니다. API의 wrapper 및 DTO 차이는 `models/normalizers.js`에서 흡수합니다.

## API 매핑

취소된 `API-001`, `API-003`, `API-004`는 호출하지 않습니다. 작품 키워드 검색은 현재 유효한 `API-031`을 사용합니다.

| 기능 | Method | Endpoint |
|---|---:|---|
| 작품별 촬영지 | GET | `/dramas/{dramaId}` |
| 촬영지 상세 | GET | `/scenes/{sceneId}` |
| 위시리스트 추가·삭제 | POST / DELETE | `/wishlist/{sceneId}` |
| 위시리스트 작품 | GET | `/wishlist/dramas` |
| 작품별 위시리스트 촬영지 | GET | `/wishlist/dramas/{dramaId}/scenes` |
| 여행 계획 생성·목록 | POST / GET | `/plans` |
| 여행 계획 상세·수정·삭제 | GET / PUT / DELETE | `/plans/{planId}` |
| 여행 세부 일정 동기화 | PUT | `/plans/{planId}/details` |
| 장소 상세 | GET | `/places/{placeId}` |
| 촬영지 주변 장소 | GET | `/scenes/{sceneId}/nearby-attractions` |
| 로그인 | POST | `/auth/login` |
| 회원가입 | POST | `/members` |
| 내 정보 | GET | `/me` |
| 연도 목차 | GET | `/dramas/years` |
| 연도별 작품 | GET | `/dramas/years/{year}` |
| 장르 목차 | GET | `/dramas/genres` |
| 장르별 작품 | GET | `/dramas/genres/{genreId}` |
| 작품 키워드 검색 | GET | `/dramas/search` |
| 조건 기반 장소 검색 | GET | `/places/search` |

Axios의 `baseURL`이 `/api/v1`까지 포함하므로 서비스 코드에는 그 이후 경로만 기록합니다.

## 주요 변경 지점

- 핀 기본 색상: `src/config/app.js`의 `PLAN_COLORS`
- 장소 카테고리: `src/config/app.js`의 `PLACE_CATEGORIES`
- API DTO 대응: `src/models/normalizers.js`
- API Endpoint: `src/api/services.js`
- 전체 디자인 토큰: `src/assets/styles.css`의 `:root`
- 카카오맵 렌더링: `src/components/map/KakaoMap.vue`
- 로그인 후 동기화: `src/stores/sync.js`, `src/views/SyncView.vue`

## 저장소 키

- `pilgrimage.auth`: 인증 토큰과 사용자 요약
- `pilgrimage.guest`: 비회원 위시리스트 및 여행 계획
- `pilgrimage.plan-colors`: 서버 계획 ID별 프론트엔드 핀 색상
- `pilgrimage.editor-draft`: 전역 일정 편집기 임시 상태
