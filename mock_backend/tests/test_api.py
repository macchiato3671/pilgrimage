from __future__ import annotations

import json
import threading
import time
import unittest
import urllib.error
import urllib.request

import uvicorn

from main import app


BASE_URL = "http://127.0.0.1:8765"


class ApiClient:
    def request(self, method: str, path: str, body: dict | None = None, token: str | None = None):
        data = None
        headers = {}
        if body is not None:
            data = json.dumps(body).encode("utf-8")
            headers["Content-Type"] = "application/json"
        if token:
            headers["Authorization"] = f"Bearer {token}"

        request = urllib.request.Request(f"{BASE_URL}{path}", data=data, headers=headers, method=method)
        try:
            with urllib.request.urlopen(request, timeout=5) as response:
                raw = response.read().decode("utf-8")
                return response.status, json.loads(raw) if raw else None
        except urllib.error.HTTPError as exc:
            raw = exc.read().decode("utf-8")
            return exc.code, json.loads(raw) if raw else None

    def get(self, path: str, token: str | None = None):
        return self.request("GET", path, token=token)

    def post(self, path: str, body: dict | None = None, token: str | None = None):
        return self.request("POST", path, body=body, token=token)

    def put(self, path: str, body: dict, token: str | None = None):
        return self.request("PUT", path, body=body, token=token)

    def patch(self, path: str, body: dict, token: str | None = None):
        return self.request("PATCH", path, body=body, token=token)

    def delete(self, path: str, body: dict | None = None, token: str | None = None):
        return self.request("DELETE", path, body=body, token=token)


class MockApiTestCase(unittest.TestCase):
    client = ApiClient()
    server: uvicorn.Server
    server_thread: threading.Thread

    @classmethod
    def setUpClass(cls) -> None:
        config = uvicorn.Config(app, host="127.0.0.1", port=8765, log_level="critical")
        cls.server = uvicorn.Server(config)
        cls.server_thread = threading.Thread(target=cls.server.run, daemon=True)
        cls.server_thread.start()

        deadline = time.time() + 5
        while time.time() < deadline:
            try:
                status, _ = cls.client.get("/")
                if status == 200:
                    return
            except Exception:
                time.sleep(0.1)
        raise RuntimeError("Test server did not start.")

    @classmethod
    def tearDownClass(cls) -> None:
        cls.server.should_exit = True
        cls.server_thread.join(timeout=5)

    def test_public_drama_and_scene_apis(self) -> None:
        status, body = self.client.get("/api/v1/dramas")
        self.assertEqual(status, 200)
        self.assertGreaterEqual(len(body["dramas"]), 2)
        first_drama = body["dramas"][0]
        self.assertEqual(set(first_drama), {"id", "title", "genres", "posterUrl", "releasedAt"})
        self.assertIsInstance(first_drama["id"], int)
        self.assertIn("releasedAt", first_drama)
        self.assertIn("genres", first_drama)
        self.assertGreaterEqual(len(first_drama["genres"]), 1)
        self.assertIn("genreId", first_drama["genres"][0])
        self.assertIn("name", first_drama["genres"][0])

        status, body = self.client.get("/api/v1/dramas/1")
        self.assertEqual(status, 200)
        self.assertEqual(body["dramaId"], 1)
        self.assertGreaterEqual(len(body["scenes"]), 1)

        status, body = self.client.get("/api/v1/scenes/1")
        self.assertEqual(status, 200)
        self.assertEqual(body["sceneId"], 1)

        status, body = self.client.get("/api/v1/scenes/1/nearby-attractions?radiusKm=3&page=0&size=10")
        self.assertEqual(status, 200)
        self.assertIn("attractions", body)

    def test_drama_order_condition_sorts_by_year_and_genre(self) -> None:
        status, body = self.client.get("/api/v1/dramas?OrderCondition=YEAR")
        self.assertEqual(status, 200)
        released_dates = [drama["releasedAt"] for drama in body["dramas"]]
        self.assertEqual(released_dates, sorted(released_dates))
        self.assertTrue(all(set(drama) == {"id", "title", "genres", "posterUrl", "releasedAt"} for drama in body["dramas"]))

        status, body = self.client.get("/api/v1/dramas?OrderCondition=GENRE")
        self.assertEqual(status, 200)
        genres = [drama["genres"][0]["name"] for drama in body["dramas"]]
        self.assertEqual(genres, sorted(genres))

    def test_excluded_apis_return_404(self) -> None:
        status, _ = self.client.get("/api/v1/dramas/search")
        self.assertEqual(status, 404)

        status, _ = self.client.get("/api/v1/dramas/1/scenes")
        self.assertEqual(status, 404)

    def test_login_and_auth_required_api(self) -> None:
        status, body = self.client.post(
            "/api/v1/auth/login",
            {"email": "member@example.com", "password": "Password123!"},
        )
        self.assertEqual(status, 200)
        self.assertEqual(body["accessToken"], "user-token")

        status, _ = self.client.get("/api/v1/me")
        self.assertEqual(status, 401)

        status, body = self.client.get("/api/v1/me", token=body["accessToken"])
        self.assertEqual(status, 200)
        self.assertEqual(body["role"], "USER")

    def test_login_failures_return_401(self) -> None:
        for email, password in (
            ("member@example.com", "wrong-password"),
            ("missing@example.com", "Password123!"),
        ):
            with self.subTest(email=email):
                status, body = self.client.post(
                    "/api/v1/auth/login",
                    {"email": email, "password": password},
                )
                self.assertEqual(status, 401)
                self.assertEqual(body["detail"]["errorCode"], "INVALID_CREDENTIALS")
                self.assertEqual(body["detail"]["message"], "Invalid email or password.")

    def test_wishlist_mutation_flow(self) -> None:
        status, body = self.client.post("/api/v1/wishlist/2", token="user-token")
        self.assertEqual(status, 201)
        wishlist_id = body["wishlistId"]

        status, body = self.client.get("/api/v1/wishlist", token="user-token")
        self.assertEqual(status, 200)
        wishlist = next(item for item in body["wishlists"] if item["wishlistId"] == wishlist_id)
        self.assertIn("createdAt", wishlist)
        self.assertIn("scene", wishlist)
        self.assertNotIn("memberId", wishlist)
        self.assertNotIn("sceneId", wishlist)
        self.assertEqual(wishlist["scene"]["sceneId"], 2)

        status, body = self.client.delete("/api/v1/wishlist/2", token="user-token")
        self.assertEqual(status, 200)
        self.assertEqual(body["sceneId"], 2)
        self.assertTrue(body["deleted"])

    def test_travel_plan_mutation_flow(self) -> None:
        create_body = {
            "title": "자동 테스트 일정",
            "beginDate": "2026-06-20",
            "endDate": "2026-06-21",
            "details": [{"dayNo": 1, "beginTime": "10:00", "endTime": "11:00", "sceneId": 1}],
        }
        status, body = self.client.post("/api/v1/plans", create_body, token="user-token")
        self.assertEqual(status, 201)
        plan_id = body["planId"]

        status, body = self.client.get(f"/api/v1/plans/{plan_id}", token="user-token")
        self.assertEqual(status, 200)
        self.assertEqual(body["title"], "자동 테스트 일정")

        update_body = {"title": "수정된 자동 테스트 일정", "beginDate": "2026-06-22", "endDate": "2026-06-23", "details": []}
        status, body = self.client.put(f"/api/v1/plans/{plan_id}", update_body, token="user-token")
        self.assertEqual(status, 200)
        self.assertEqual(body["title"], "수정된 자동 테스트 일정")

        status, body = self.client.delete(f"/api/v1/plans/{plan_id}", token="user-token")
        self.assertEqual(status, 200)
        self.assertTrue(body["deleted"])

    def test_admin_role_enforcement(self) -> None:
        status, _ = self.client.get("/api/v1/members", token="user-token")
        self.assertEqual(status, 403)

        status, body = self.client.get("/api/v1/members", token="admin-token")
        self.assertEqual(status, 200)
        self.assertGreaterEqual(len(body["members"]), 2)

        status, body = self.client.patch("/api/v1/members/1", {"nickname": "관리자수정"}, token="admin-token")
        self.assertEqual(status, 200)
        self.assertEqual(body["nickname"], "관리자수정")


if __name__ == "__main__":
    unittest.main()
