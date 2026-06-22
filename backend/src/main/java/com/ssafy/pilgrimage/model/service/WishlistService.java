package com.ssafy.pilgrimage.model.service;

import com.ssafy.pilgrimage.model.dto.response.WishlistResponseDto;

public interface WishlistService {

	void addWishlist(int memberId, int sceneId);

	WishlistResponseDto getWishlist(int memberId);

	void deleteWishlist(int memberId, int sceneId);

}
