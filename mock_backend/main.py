from __future__ import annotations

from copy import deepcopy
from datetime import datetime, timezone
from typing import Any, Literal

from fastapi import Depends, FastAPI, Header, HTTPException, Path, Query, status
from fastapi.middleware.cors import CORSMiddleware
from pydantic import BaseModel, Field


app = FastAPI(title="Pilgrimage Mock API", version="1.0.0")

app.add_middleware(
    CORSMiddleware,
    allow_origins=["*"],
    allow_credentials=True,
    allow_methods=["*"],
    allow_headers=["*"],
)


Role = Literal["USER", "ADMIN"]


def now_iso() -> str:
    return datetime.now(timezone.utc).replace(microsecond=0).isoformat().replace("+00:00", "Z")


def error(status_code: int, error_code: str, message: str) -> HTTPException:
    return HTTPException(status_code=status_code, detail={"errorCode": error_code, "message": message})


class LoginRequest(BaseModel):
    email: str = Field(min_length=1)
    password: str = Field(min_length=1)


class SignupRequest(BaseModel):
    email: str = Field(min_length=1)
    password: str = Field(min_length=1)
    nickname: str = Field(min_length=1)


class MemberUpdateRequest(BaseModel):
    email: str | None = None
    nickname: str | None = None
    currentPassword: str | None = None
    newPassword: str | None = None
    role: Role | None = None
    status: str | None = None


class WithdrawRequest(BaseModel):
    password: str | None = None
    reason: str | None = None


class PlanDetailRequest(BaseModel):
    dayNo: int
    beginTime: str
    endTime: str
    sceneId: int | None = None
    placeId: int | None = None


class PlanRequest(BaseModel):
    title: str = Field(min_length=1)
    beginDate: str
    endDate: str
    details: list[PlanDetailRequest] = []


def member_payload(member: dict[str, Any]) -> dict[str, Any]:
    return {
        "memberId": member["memberId"],
        "email": member["email"],
        "nickname": member["nickname"],
        "role": member["role"],
        "status": member["status"],
        "createdAt": member["createdAt"],
    }


def scene_payload(scene: dict[str, Any]) -> dict[str, Any]:
    return {
        "sceneId": scene["sceneId"],
        "dramaId": scene["dramaId"],
        "name": scene["name"],
        "description": scene["description"],
        "address": scene["address"],
        "latitude": scene["latitude"],
        "longitude": scene["longitude"],
        "imgUrl": scene["imgUrl"],
    }


def place_payload(place: dict[str, Any]) -> dict[str, Any]:
    return {
        "placeId": place["placeId"],
        "name": place["name"],
        "description": place["description"],
        "address": place["address"],
        "latitude": place["latitude"],
        "longitude": place["longitude"],
        "imgUrl": place["imgUrl"],
        "contentId": place["contentId"],
        "contentTypeId": place["contentTypeId"],
        "contentTypeName": place["contentTypeName"],
    }


def plan_payload(plan: dict[str, Any]) -> dict[str, Any]:
    return {
        "planId": plan["planId"],
        "memberId": plan["memberId"],
        "title": plan["title"],
        "beginDate": plan["beginDate"],
        "endDate": plan["endDate"],
        "details": deepcopy(plan["details"]),
        "createdAt": plan["createdAt"],
        "updatedAt": plan["updatedAt"],
    }


state: dict[str, Any] = {
    "nextMemberId": 3,
    "nextWishlistId": 2,
    "nextPlanId": 2,
    "nextDetailId": 3,
    "members": {
        1: {
            "memberId": 1,
            "email": "member@example.com",
            "password": "Password123!",
            "nickname": "여행자",
            "role": "USER",
            "status": "ACTIVE",
            "createdAt": "2026-06-03T10:00:00",
        },
        2: {
            "memberId": 2,
            "email": "admin@example.com",
            "password": "Admin123!",
            "nickname": "관리자",
            "role": "ADMIN",
            "status": "ACTIVE",
            "createdAt": "2026-06-03T09:00:00",
        },
    },
    "tokens": {"user-token": 1, "admin-token": 2},
    "dramas": {
        1: {
            "dramaId": 1,
            "title": "선재 업고 튀어",
            "name": "선재 업고 튀어",
            "description": "서울과 수원을 배경으로 한 청춘 로맨스 드라마입니다.",
            "genres": [
                {"genreId": 1, "name": "로맨스"},
                {"genreId": 2, "name": "판타지"},
            ],
            "posterUrl": "https://picsum.photos/seed/drama-1/320/480",
            "imgUrl": "https://picsum.photos/seed/drama-1/640/360",
            "releasedAt": "2024-01-01",
        },
        2: {
            "dramaId": 2,
            "title": "이상한 변호사 우영우",
            "name": "이상한 변호사 우영우",
            "description": "제주와 서울의 장소를 담은 휴먼 법정 드라마입니다.",
            "genres": [
                {"genreId": 3, "name": "휴먼"},
                {"genreId": 4, "name": "법정"},
            ],
            "posterUrl": "https://picsum.photos/seed/drama-2/320/480",
            "imgUrl": "https://picsum.photos/seed/drama-2/640/360",
            "releasedAt": "2022-06-29",
        },
    },
    "scenes": {
        1: {
            "sceneId": 1,
            "dramaId": 1,
            "name": "솔이와 선재의 골목",
            "description": "주인공들이 처음 마음을 확인하는 골목길 장면입니다.",
            "address": "서울특별시 종로구 자하문로",
            "latitude": 37.5826,
            "longitude": 126.9706,
            "imgUrl": "https://picsum.photos/seed/scene-1/640/360",
        },
        2: {
            "sceneId": 2,
            "dramaId": 1,
            "name": "콘서트장 앞",
            "description": "팬들과 밴드가 만나는 콘서트장 주변 장면입니다.",
            "address": "서울특별시 송파구 올림픽로",
            "latitude": 37.5151,
            "longitude": 127.0729,
            "imgUrl": "https://picsum.photos/seed/scene-2/640/360",
        },
        3: {
            "sceneId": 3,
            "dramaId": 2,
            "name": "제주 바닷가 산책로",
            "description": "고래 이야기가 이어지는 바닷가 산책 장면입니다.",
            "address": "제주특별자치도 서귀포시 성산읍",
            "latitude": 33.4581,
            "longitude": 126.9426,
            "imgUrl": "https://picsum.photos/seed/scene-3/640/360",
        },
    },
    "places": {
        1: {
            "placeId": 1,
            "name": "경복궁",
            "description": "서울 도심의 대표 궁궐 관광지입니다.",
            "address": "서울특별시 종로구 사직로 161",
            "latitude": 37.5796,
            "longitude": 126.9770,
            "imgUrl": "https://picsum.photos/seed/place-1/640/360",
            "contentId": 126508,
            "contentTypeId": 12,
            "contentTypeName": "관광지",
            "distanceKm": 0.8,
        },
        2: {
            "placeId": 2,
            "name": "석촌호수",
            "description": "산책하기 좋은 도심 호수입니다.",
            "address": "서울특별시 송파구 잠실동",
            "latitude": 37.5112,
            "longitude": 127.0981,
            "imgUrl": "https://picsum.photos/seed/place-2/640/360",
            "contentId": 129223,
            "contentTypeId": 12,
            "contentTypeName": "관광지",
            "distanceKm": 1.2,
        },
        3: {
            "placeId": 3,
            "name": "성산일출봉",
            "description": "제주의 대표 자연 명소입니다.",
            "address": "제주특별자치도 서귀포시 성산읍",
            "latitude": 33.4589,
            "longitude": 126.9420,
            "imgUrl": "https://picsum.photos/seed/place-3/640/360",
            "contentId": 127203,
            "contentTypeId": 12,
            "contentTypeName": "관광지",
            "distanceKm": 0.3,
        },
    },
    "wishlist": {
        1: {"wishlistId": 1, "memberId": 1, "sceneId": 1, "createdAt": "2026-06-03T11:00:00"}
    },
    "plans": {
        1: {
            "planId": 1,
            "memberId": 1,
            "title": "서울 드라마 여행",
            "beginDate": "2026-06-10",
            "endDate": "2026-06-12",
            "details": [
                {"detailId": 1, "dayNo": 1, "beginTime": "10:00", "endTime": "11:30", "sceneId": 1, "placeId": None},
                {"detailId": 2, "dayNo": 1, "beginTime": "13:00", "endTime": "15:00", "sceneId": None, "placeId": 1},
            ],
            "createdAt": "2026-06-03T12:00:00",
            "updatedAt": "2026-06-03T12:00:00",
        }
    },
}


def current_member(authorization: str | None = Header(default=None, alias="Authorization")) -> dict[str, Any]:
    if not authorization or not authorization.startswith("Bearer "):
        raise error(status.HTTP_401_UNAUTHORIZED, "UNAUTHORIZED", "Authorization header is required.")
    token = authorization.removeprefix("Bearer ").strip()
    member_id = state["tokens"].get(token)
    member = state["members"].get(member_id)
    if not member or member["status"] != "ACTIVE":
        raise error(status.HTTP_401_UNAUTHORIZED, "INVALID_OR_EXPIRED_TOKEN", "Invalid or expired token.")
    return member


def admin_member(member: dict[str, Any] = Depends(current_member)) -> dict[str, Any]:
    if member["role"] != "ADMIN":
        raise error(status.HTTP_403_FORBIDDEN, "ADMIN_AUTHORITY_REQUIRED", "Admin authority is required.")
    return member


def get_scene(scene_id: int) -> dict[str, Any]:
    scene = state["scenes"].get(scene_id)
    if not scene:
        raise error(status.HTTP_404_NOT_FOUND, "SCENE_NOT_FOUND", "Scene not found.")
    return scene


def get_plan_for_member(plan_id: int, member: dict[str, Any]) -> dict[str, Any]:
    plan = state["plans"].get(plan_id)
    if not plan:
        raise error(status.HTTP_404_NOT_FOUND, "TRAVEL_PLAN_NOT_FOUND", "Travel plan not found.")
    if member["role"] != "ADMIN" and plan["memberId"] != member["memberId"]:
        raise error(status.HTTP_403_FORBIDDEN, "TRAVEL_PLAN_ACCESS_DENIED", "Travel plan access is denied.")
    return plan


@app.get("/")
def root() -> dict[str, Any]:
    return {
        "name": "Pilgrimage Mock API",
        "docs": "/docs",
        "implemented": ["API-001", "API-002", "API-005..API-024"],
        "excluded": ["API-003", "API-004"],
    }


@app.get("/api/v1/dramas")
def list_dramas(
    keyword: str | None = Query(default=None),
    orderCondition: str | None = Query(default=None, alias="OrderCondition"),
) -> dict[str, Any]:
    dramas = list(state["dramas"].values())
    if keyword:
        lowered = keyword.lower()
        dramas = [drama for drama in dramas if lowered in drama["title"].lower() or lowered in drama["description"].lower()]
    if orderCondition and orderCondition.upper() in {"DESC", "LATEST"}:
        dramas = list(reversed(dramas))
    return {"dramas": deepcopy(dramas)}


@app.get("/api/v1/dramas/search", include_in_schema=False)
def excluded_drama_search() -> None:
    raise error(status.HTTP_404_NOT_FOUND, "API_NOT_IMPLEMENTED", "API-003 is excluded from the mock server.")


@app.get("/api/v1/dramas/{drama_id}")
def list_drama_scenes(drama_id: int) -> dict[str, Any]:
    drama = state["dramas"].get(drama_id)
    if not drama:
        raise error(status.HTTP_404_NOT_FOUND, "DRAMA_NOT_FOUND", "Drama not found.")
    scenes = [scene_payload(scene) for scene in state["scenes"].values() if scene["dramaId"] == drama_id]
    return {"dramaId": drama_id, "drama": deepcopy(drama), "scenes": scenes}


@app.get("/api/v1/scenes/{scene_id}")
def get_scene_detail(scene_id: int) -> dict[str, Any]:
    return scene_payload(get_scene(scene_id))


@app.get("/api/v1/scenes/{scene_id}/nearby-attractions")
def nearby_attractions(
    scene_id: int,
    radius_km: float = Query(default=3, alias="radiusKm"),
    content_type_id: int | None = Query(default=None, alias="contentTypeId"),
    page: int = Query(default=0, ge=0),
    size: int = Query(default=10, ge=1),
) -> dict[str, Any]:
    scene = get_scene(scene_id)
    attractions = list(state["places"].values())
    if content_type_id is not None:
        attractions = [place for place in attractions if place["contentTypeId"] == content_type_id]
    attractions = [place for place in attractions if place["distanceKm"] <= radius_km]
    start = page * size
    items = attractions[start : start + size]
    total = len(attractions)
    return {
        "sceneId": scene["sceneId"],
        "sceneName": scene["name"],
        "sceneLatitude": scene["latitude"],
        "sceneLongitude": scene["longitude"],
        "radiusKm": radius_km,
        "attractions": [{**place_payload(place), "distanceKm": place["distanceKm"]} for place in items],
        "page": page,
        "size": size,
        "totalElements": total,
        "totalPages": (total + size - 1) // size,
        "hasNext": start + size < total,
    }


@app.get("/api/v1/places/{place_id}")
def get_place_detail(place_id: int) -> dict[str, Any]:
    place = state["places"].get(place_id)
    if not place:
        raise error(status.HTTP_404_NOT_FOUND, "PLACE_NOT_FOUND", "Place not found.")
    return place_payload(place)


@app.post("/api/v1/wishlist/{scene_id}", status_code=status.HTTP_201_CREATED)
def add_wishlist(scene_id: int, member: dict[str, Any] = Depends(current_member)) -> dict[str, Any]:
    scene = get_scene(scene_id)
    for item in state["wishlist"].values():
        if item["memberId"] == member["memberId"] and item["sceneId"] == scene_id:
            return {**deepcopy(item), "scene": scene_payload(scene)}
    wishlist_id = state["nextWishlistId"]
    state["nextWishlistId"] += 1
    item = {"wishlistId": wishlist_id, "memberId": member["memberId"], "sceneId": scene_id, "createdAt": now_iso()}
    state["wishlist"][wishlist_id] = item
    return {**deepcopy(item), "scene": scene_payload(scene)}


@app.get("/api/v1/wishlist")
def get_wishlist(member: dict[str, Any] = Depends(current_member)) -> dict[str, Any]:
    items = []
    for item in state["wishlist"].values():
        if item["memberId"] == member["memberId"]:
            scene = state["scenes"].get(item["sceneId"])
            items.append({**deepcopy(item), "scene": scene_payload(scene)} if scene else deepcopy(item))
    return {"wishlist": items}


@app.delete("/api/v1/wishlist/{wishlist_id}")
def remove_wishlist(
    wishlist_id: int,
    member: dict[str, Any] = Depends(current_member),
) -> dict[str, Any]:
    item = state["wishlist"].get(wishlist_id)
    if not item:
        raise error(status.HTTP_404_NOT_FOUND, "WISHLIST_NOT_FOUND", "Wishlist item not found.")
    if member["role"] != "ADMIN" and item["memberId"] != member["memberId"]:
        raise error(status.HTTP_403_FORBIDDEN, "WISHLIST_ACCESS_DENIED", "Wishlist access is denied.")
    del state["wishlist"][wishlist_id]
    return {"wishlistId": wishlist_id, "deleted": True}


@app.post("/api/v1/plans", status_code=status.HTTP_201_CREATED)
def create_plan(payload: PlanRequest, member: dict[str, Any] = Depends(current_member)) -> dict[str, Any]:
    plan_id = state["nextPlanId"]
    state["nextPlanId"] += 1
    details = []
    for detail in payload.details:
        detail_id = state["nextDetailId"]
        state["nextDetailId"] += 1
        details.append({"detailId": detail_id, **detail.model_dump()})
    created = now_iso()
    plan = {
        "planId": plan_id,
        "memberId": member["memberId"],
        "title": payload.title,
        "beginDate": payload.beginDate,
        "endDate": payload.endDate,
        "details": details,
        "createdAt": created,
        "updatedAt": created,
    }
    state["plans"][plan_id] = plan
    return plan_payload(plan)


@app.get("/api/v1/plans")
def list_plans(member: dict[str, Any] = Depends(current_member)) -> dict[str, Any]:
    plans = [
        plan_payload(plan)
        for plan in state["plans"].values()
        if member["role"] == "ADMIN" or plan["memberId"] == member["memberId"]
    ]
    return {"plans": plans}


@app.get("/api/v1/plans/{plan_id}")
def get_plan(plan_id: int, member: dict[str, Any] = Depends(current_member)) -> dict[str, Any]:
    return plan_payload(get_plan_for_member(plan_id, member))


@app.put("/api/v1/plans/{plan_id}")
def update_plan(
    payload: PlanRequest,
    plan_id: int,
    member: dict[str, Any] = Depends(current_member),
) -> dict[str, Any]:
    plan = get_plan_for_member(plan_id, member)
    details = []
    for detail in payload.details:
        detail_id = state["nextDetailId"]
        state["nextDetailId"] += 1
        details.append({"detailId": detail_id, **detail.model_dump()})
    plan.update(
        {
            "title": payload.title,
            "beginDate": payload.beginDate,
            "endDate": payload.endDate,
            "details": details,
            "updatedAt": now_iso(),
        }
    )
    return plan_payload(plan)


@app.delete("/api/v1/plans/{plan_id}")
def delete_plan(plan_id: int, member: dict[str, Any] = Depends(current_member)) -> dict[str, Any]:
    get_plan_for_member(plan_id, member)
    del state["plans"][plan_id]
    return {"planId": plan_id, "deleted": True}


@app.post("/api/v1/auth/login")
def login(payload: LoginRequest) -> dict[str, Any]:
    member = next((item for item in state["members"].values() if item["email"] == payload.email), None)
    if not member or member["password"] != payload.password:
        raise error(status.HTTP_401_UNAUTHORIZED, "INVALID_CREDENTIALS", "Invalid email or password.")
    if member["status"] != "ACTIVE":
        raise error(status.HTTP_403_FORBIDDEN, "MEMBER_ACCESS_DENIED", "Member access is denied.")
    access_token = "admin-token" if member["role"] == "ADMIN" else "user-token"
    state["tokens"][access_token] = member["memberId"]
    return {
        "accessToken": access_token,
        "refreshToken": f"refresh-{access_token}",
        "tokenType": "Bearer",
        "expiresIn": 3600,
        "member": member_payload(member),
    }


@app.post("/api/v1/members", status_code=status.HTTP_201_CREATED)
def signup(payload: SignupRequest) -> dict[str, Any]:
    if any(member["email"] == payload.email for member in state["members"].values()):
        raise error(status.HTTP_409_CONFLICT, "EMAIL_ALREADY_EXISTS", "Email already exists.")
    member_id = state["nextMemberId"]
    state["nextMemberId"] += 1
    member = {
        "memberId": member_id,
        "email": payload.email,
        "password": payload.password,
        "nickname": payload.nickname,
        "role": "USER",
        "status": "ACTIVE",
        "createdAt": now_iso(),
    }
    state["members"][member_id] = member
    state["tokens"][f"user-token-{member_id}"] = member_id
    return member_payload(member)


@app.get("/api/v1/me")
def get_me(member: dict[str, Any] = Depends(current_member)) -> dict[str, Any]:
    return member_payload(member)


@app.patch("/api/v1/me")
def update_me(payload: MemberUpdateRequest, member: dict[str, Any] = Depends(current_member)) -> dict[str, Any]:
    if payload.email is not None:
        member["email"] = payload.email
    if payload.nickname is not None:
        member["nickname"] = payload.nickname
    if payload.newPassword is not None:
        member["password"] = payload.newPassword
    return member_payload(member)


@app.delete("/api/v1/me")
def delete_me(payload: WithdrawRequest | None = None, member: dict[str, Any] = Depends(current_member)) -> dict[str, Any]:
    member["status"] = "WITHDRAWN"
    return {"memberId": member["memberId"], "status": member["status"], "deleted": True, "reason": payload.reason if payload else None}


@app.get("/api/v1/members")
def admin_list_members(_: dict[str, Any] = Depends(admin_member)) -> dict[str, Any]:
    return {"members": [member_payload(member) for member in state["members"].values()]}


@app.get("/api/v1/members/{member_id}")
def admin_get_member(member_id: int, _: dict[str, Any] = Depends(admin_member)) -> dict[str, Any]:
    member = state["members"].get(member_id)
    if not member:
        raise error(status.HTTP_404_NOT_FOUND, "MEMBER_NOT_FOUND", "Member not found.")
    return member_payload(member)


@app.patch("/api/v1/members/{member_id}")
def admin_update_member(
    payload: MemberUpdateRequest,
    member_id: int,
    _: dict[str, Any] = Depends(admin_member),
) -> dict[str, Any]:
    member = state["members"].get(member_id)
    if not member:
        raise error(status.HTTP_404_NOT_FOUND, "MEMBER_NOT_FOUND", "Member not found.")
    for field in ("email", "nickname", "role", "status"):
        value = getattr(payload, field)
        if value is not None:
            member[field] = value
    if payload.newPassword is not None:
        member["password"] = payload.newPassword
    return member_payload(member)


@app.delete("/api/v1/members/{member_id}")
def admin_delete_member(member_id: int, admin: dict[str, Any] = Depends(admin_member)) -> dict[str, Any]:
    if member_id == admin["memberId"]:
        raise error(status.HTTP_400_BAD_REQUEST, "CANNOT_DELETE_SELF", "Cannot delete self.")
    member = state["members"].get(member_id)
    if not member:
        raise error(status.HTTP_404_NOT_FOUND, "MEMBER_NOT_FOUND", "Member not found.")
    member["status"] = "WITHDRAWN"
    return {"memberId": member_id, "status": member["status"], "deleted": True}
