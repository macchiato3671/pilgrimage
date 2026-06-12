from typing import Any

from fastapi import Depends, FastAPI, Query, status
from fastapi.middleware.cors import CORSMiddleware

from data import state
from models import LoginRequest, MemberUpdateRequest, PlanRequest, SignupRequest, WithdrawRequest
from utils import Member, admin_member, current_member, drama_list_payload, error, find_wishlist, get_plan_for_member
from utils import get_scene, member_payload, next_id, now_iso, place_payload, plan_detail_payload, plan_payload, plan_values
from utils import scene_payload, update_present, wishlist_payload, wishlist_with_scene


app = FastAPI(title="Pilgrimage Mock API", version="1.0.0")
app.add_middleware(CORSMiddleware, allow_origins=["*"], allow_credentials=True, allow_methods=["*"], allow_headers=["*"])


@app.get("/")
def root() -> dict[str, Any]:
    return {"name": "Pilgrimage Mock API", "docs": "/docs", "implemented": ["API-001", "API-002", "API-005..API-024"], "excluded": ["API-003", "API-004"]}


@app.get("/api/v1/dramas")
def list_dramas(
    keyword: str | None = Query(default=None),
    orderCondition: str | None = Query(default=None, alias="OrderCondition"),
) -> dict[str, Any]:
    dramas = list(state["dramas"].values())
    if keyword:
        lowered = keyword.lower()
        dramas = [drama for drama in dramas if lowered in drama["title"].lower() or lowered in drama["description"].lower()]
    if orderCondition:
        order_condition = orderCondition.upper()
        if order_condition == "YEAR":
            dramas = sorted(dramas, key=lambda drama: drama["releasedAt"])
        elif order_condition == "GENRE":
            dramas = sorted(dramas, key=lambda drama: drama["genres"][0]["name"] if drama["genres"] else "")
        elif order_condition in {"DESC", "LATEST"}:
            dramas = list(reversed(dramas))
    return {"dramas": [drama_list_payload(drama) for drama in dramas]}


@app.get("/api/v1/dramas/search", include_in_schema=False)
def excluded_drama_search() -> None:
    raise error(status.HTTP_404_NOT_FOUND, "API_NOT_IMPLEMENTED", "API-003 is excluded from the mock server.")


@app.get("/api/v1/dramas/{drama_id}")
def list_drama_scenes(drama_id: int) -> dict[str, Any]:
    drama = state["dramas"].get(drama_id)
    if not drama:
        raise error(status.HTTP_404_NOT_FOUND, "DRAMA_NOT_FOUND", "Drama not found.")
    scenes = [scene_payload(scene) for scene in state["scenes"].values() if scene["dramaId"] == drama_id]
    return {"dramaId": drama_id, "drama": drama, "scenes": scenes}


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
def add_wishlist(scene_id: int, member: Member = Depends(current_member)) -> dict[str, Any]:
    scene = get_scene(scene_id)
    if found := find_wishlist(member["memberId"], scene_id):
        return wishlist_with_scene(found[1], scene)
    wishlist_id = next_id("nextWishlistId")
    item = {"wishlistId": wishlist_id, "memberId": member["memberId"], "sceneId": scene_id, "createdAt": now_iso()}
    state["wishlist"][wishlist_id] = item
    return wishlist_with_scene(item, scene)


@app.get("/api/v1/wishlist")
def get_wishlist(member: Member = Depends(current_member)) -> dict[str, Any]:
    return {"wishlists": [wishlist_payload(item) for item in state["wishlist"].values() if item["memberId"] == member["memberId"]]}


@app.delete("/api/v1/wishlist/{scene_id}")
def remove_wishlist(
    scene_id: int,
    member: Member = Depends(current_member),
) -> dict[str, Any]:
    get_scene(scene_id)
    found = find_wishlist(member["memberId"], scene_id)
    if not found:
        raise error(status.HTTP_404_NOT_FOUND, "WISHLIST_NOT_FOUND", "Wishlist item not found.")
    del state["wishlist"][found[0]]
    return {"sceneId": scene_id, "deleted": True}


@app.post("/api/v1/plans", status_code=status.HTTP_201_CREATED)
def create_plan(payload: PlanRequest, member: Member = Depends(current_member)) -> dict[str, Any]:
    plan_id = next_id("nextPlanId")
    created = now_iso()
    plan = {"planId": plan_id, "memberId": member["memberId"], **plan_values(payload), "createdAt": created, "updatedAt": created}
    state["plans"][plan_id] = plan
    return plan_payload(plan)


@app.get("/api/v1/plans")
def list_plans(member: Member = Depends(current_member)) -> dict[str, Any]:
    plans = [
        plan_payload(plan)
        for plan in state["plans"].values()
        if member["role"] == "ADMIN" or plan["memberId"] == member["memberId"]
    ]
    return {"plans": plans}


@app.get("/api/v1/plans/{plan_id}")
def get_plan(plan_id: int, member: Member = Depends(current_member)) -> dict[str, Any]:
    return plan_detail_payload(get_plan_for_member(plan_id, member))


@app.put("/api/v1/plans/{plan_id}")
def update_plan(
    payload: PlanRequest,
    plan_id: int,
    member: Member = Depends(current_member),
) -> dict[str, Any]:
    plan = get_plan_for_member(plan_id, member)
    plan.update({**plan_values(payload), "updatedAt": now_iso()})
    return plan_payload(plan)


@app.delete("/api/v1/plans/{plan_id}")
def delete_plan(plan_id: int, member: Member = Depends(current_member)) -> dict[str, Any]:
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
    member_id = next_id("nextMemberId")
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
def get_me(member: Member = Depends(current_member)) -> dict[str, Any]:
    return member_payload(member)


@app.patch("/api/v1/me")
def update_me(payload: MemberUpdateRequest, member: Member = Depends(current_member)) -> dict[str, Any]:
    update_present(member, payload, ("email", "nickname"))
    if payload.newPassword is not None:
        member["password"] = payload.newPassword
    return member_payload(member)


@app.delete("/api/v1/me")
def delete_me(payload: WithdrawRequest | None = None, member: Member = Depends(current_member)) -> dict[str, Any]:
    member["status"] = "WITHDRAWN"
    return {"memberId": member["memberId"], "status": member["status"], "deleted": True, "reason": payload.reason if payload else None}


@app.get("/api/v1/members")
def admin_list_members(_: Member = Depends(admin_member)) -> dict[str, Any]:
    return {"members": [member_payload(member) for member in state["members"].values()]}


@app.get("/api/v1/members/{member_id}")
def admin_get_member(member_id: int, _: Member = Depends(admin_member)) -> dict[str, Any]:
    member = state["members"].get(member_id)
    if not member:
        raise error(status.HTTP_404_NOT_FOUND, "MEMBER_NOT_FOUND", "Member not found.")
    return member_payload(member)


@app.patch("/api/v1/members/{member_id}")
def admin_update_member(
    payload: MemberUpdateRequest,
    member_id: int,
    _: Member = Depends(admin_member),
) -> dict[str, Any]:
    member = state["members"].get(member_id)
    if not member:
        raise error(status.HTTP_404_NOT_FOUND, "MEMBER_NOT_FOUND", "Member not found.")
    update_present(member, payload, ("email", "nickname", "role", "status"))
    if payload.newPassword is not None:
        member["password"] = payload.newPassword
    return member_payload(member)


@app.delete("/api/v1/members/{member_id}")
def admin_delete_member(member_id: int, admin: Member = Depends(admin_member)) -> dict[str, Any]:
    if member_id == admin["memberId"]:
        raise error(status.HTTP_400_BAD_REQUEST, "CANNOT_DELETE_SELF", "Cannot delete self.")
    member = state["members"].get(member_id)
    if not member:
        raise error(status.HTTP_404_NOT_FOUND, "MEMBER_NOT_FOUND", "Member not found.")
    member["status"] = "WITHDRAWN"
    return {"memberId": member_id, "status": member["status"], "deleted": True}
