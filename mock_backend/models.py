from typing import Literal

from pydantic import BaseModel, Field


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
    role: Literal["USER", "ADMIN"] | None = None
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
