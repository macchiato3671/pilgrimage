package com.moonback.pilgrimage.model.service;

import com.moonback.pilgrimage.model.dto.response.WishlistResponseDto;

public interface WishlistService {

	void addWishlist(int memberId, int sceneId);

	WishlistResponseDto getWishlist(int memberId);

	void deleteWishlist(int memberId, int sceneId);

}
