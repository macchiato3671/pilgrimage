package com.ssafy.pilgrimage.model.type;

import lombok.AllArgsConstructor;
import lombok.Getter;

@Getter
@AllArgsConstructor
public enum MemberRole {
	USER(1),
	ADMIN(2);
	
	private final int id;
	
	public static MemberRole fromId(int id) {
        for (MemberRole role : values()) {
            if (role.id == id) {
                return role;
            }
        }

        throw new IllegalArgumentException("존재하지 않는 회원 권한입니다.");
    }
}
