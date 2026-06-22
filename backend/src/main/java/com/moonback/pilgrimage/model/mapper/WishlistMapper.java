package com.moonback.pilgrimage.model.mapper;

public interface WishlistMapper {

	int addWishlist(int memberId, int sceneId);

	int countByMemberIdAndSceneId(int memberId, int sceneId);

	void getWishlist(int memberId);

	void deleteWishlist(int memberId, int sceneId);

}
