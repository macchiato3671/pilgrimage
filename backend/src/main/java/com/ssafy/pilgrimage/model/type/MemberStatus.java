package com.ssafy.pilgrimage.model.type;

import lombok.AllArgsConstructor;
import lombok.Getter;

@Getter
@AllArgsConstructor
public enum MemberStatus {
	ACTIVE(1),
	WITHDRAWN(2),
	SUSPENDED(3);
	
	private final int id;
	
	public static MemberStatus fromId(int id) {
        for (MemberStatus status : values()) {
            if (status.id == id) {
                return status;
            }
        }

        throw new IllegalArgumentException("존재하지 않는 회원 상태입니다.");
    }
}
