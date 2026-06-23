package com.moonback.pilgrimage.model.service;

import com.moonback.pilgrimage.model.dto.response.WishlistDramaResponseDto;
import com.moonback.pilgrimage.model.dto.response.WishlistScenePageResponseDto;

public interface WishlistService {

	void addWishlist(int memberId, int sceneId);

	void deleteWishlist(int memberId, int sceneId);

	WishlistDramaResponseDto getDrama(int memberId);

	WishlistScenePageResponseDto getScene(int memberId, int dramaId, int page, int size);

}
