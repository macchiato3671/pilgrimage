from typing import Any


def by_id(key: str, *items: dict[str, Any]) -> dict[int, dict[str, Any]]:
    return {item[key]: item for item in items}


def img(seed: str, size: str = "640/360") -> str:
    return f"https://picsum.photos/seed/{seed}/{size}"


state: dict[str, Any] = {
    "nextMemberId": 3,
    "nextWishlistId": 2,
    "nextPlanId": 2,
    "nextDetailId": 3,
    "tokens": {"user-token": 1, "admin-token": 2},
    "members": by_id(
        "memberId",
        dict(memberId=1, email="member@example.com", password="Password123!", nickname="여행자", role="USER", status="ACTIVE", createdAt="2026-06-03T10:00:00"),
        dict(memberId=2, email="admin@example.com", password="Admin123!", nickname="관리자", role="ADMIN", status="ACTIVE", createdAt="2026-06-03T09:00:00"),
    ),
    "dramas": by_id(
        "dramaId",
        dict(
            dramaId=1, title="선재 업고 튀어", name="선재 업고 튀어", description="서울과 수원을 배경으로 한 청춘 로맨스 드라마입니다.",
            genres=[{"genreId": 1, "name": "로맨스"}, {"genreId": 2, "name": "판타지"}], posterUrl=img("drama-1", "320/480"), imgUrl=img("drama-1"), releasedAt="2024-01-01",
        ),
        dict(
            dramaId=2, title="이상한 변호사 우영우", name="이상한 변호사 우영우", description="제주와 서울의 장소를 담은 휴먼 법정 드라마입니다.",
            genres=[{"genreId": 3, "name": "휴먼"}, {"genreId": 4, "name": "법정"}], posterUrl=img("drama-2", "320/480"), imgUrl=img("drama-2"), releasedAt="2022-06-29",
        ),
    ),
    "scenes": by_id(
        "sceneId",
        dict(sceneId=1, dramaId=1, name="솔이와 선재의 골목", description="주인공들이 처음 마음을 확인하는 골목길 장면입니다.", address="서울특별시 종로구 자하문로", latitude=37.5826, longitude=126.9706, imgUrl=img("scene-1")),
        dict(sceneId=2, dramaId=1, name="콘서트장 앞", description="팬들과 밴드가 만나는 콘서트장 주변 장면입니다.", address="서울특별시 송파구 올림픽로", latitude=37.5151, longitude=127.0729, imgUrl=img("scene-2")),
        dict(sceneId=3, dramaId=2, name="제주 바닷가 산책로", description="고래 이야기가 이어지는 바닷가 산책 장면입니다.", address="제주특별자치도 서귀포시 성산읍", latitude=33.4581, longitude=126.9426, imgUrl=img("scene-3")),
    ),
    "places": by_id(
        "placeId",
        dict(placeId=1, name="경복궁", description="서울 도심의 대표 궁궐 관광지입니다.", address="서울특별시 종로구 사직로 161", latitude=37.5796, longitude=126.9770, imgUrl=img("place-1"), contentId=126508, contentTypeId=12, contentTypeName="관광지", distanceKm=0.8),
        dict(placeId=2, name="석촌호수", description="산책하기 좋은 도심 호수입니다.", address="서울특별시 송파구 잠실동", latitude=37.5112, longitude=127.0981, imgUrl=img("place-2"), contentId=129223, contentTypeId=12, contentTypeName="관광지", distanceKm=1.2),
        dict(placeId=3, name="성산일출봉", description="제주의 대표 자연 명소입니다.", address="제주특별자치도 서귀포시 성산읍", latitude=33.4589, longitude=126.9420, imgUrl=img("place-3"), contentId=127203, contentTypeId=12, contentTypeName="관광지", distanceKm=0.3),
    ),
    "wishlist": {1: dict(wishlistId=1, memberId=1, sceneId=1, createdAt="2026-06-03T11:00:00")},
    "plans": {
        1: dict(
            planId=1, memberId=1, title="서울 드라마 여행", beginDate="2026-06-10", endDate="2026-06-12",
            details=[
                dict(detailId=1, dayNo=1, beginTime="10:00", endTime="11:30", sceneId=1, placeId=None),
                dict(detailId=2, dayNo=1, beginTime="13:00", endTime="15:00", sceneId=None, placeId=1),
            ],
            createdAt="2026-06-03T12:00:00", updatedAt="2026-06-03T12:00:00",
        )
    },
}
