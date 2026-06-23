package com.moonback.pilgrimage.model.service;

import com.moonback.pilgrimage.model.dto.response.WishlistDramaResponseDto;
import com.moonback.pilgrimage.model.dto.response.WishlistResponseDto;
import com.moonback.pilgrimage.model.dto.response.WishlistScenePageResponseDto;
import com.moonback.pilgrimage.model.dto.response.WishlistSceneResponseDto;

public interface WishlistService {

	void addWishlist(int memberId, int sceneId);

	WishlistResponseDto getWishlist(int memberId);

	void deleteWishlist(int memberId, int sceneId);

	WishlistDramaResponseDto getDrama(int memberId);

	WishlistScenePageResponseDto getScene(int memberId, int dramaId, int page, int size);

}
