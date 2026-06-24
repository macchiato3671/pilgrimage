package com.moonback.pilgrimage.model.mapper;

import java.util.List;

import org.apache.ibatis.annotations.Param;

import com.moonback.pilgrimage.model.dto.DramaGenreRowDto;
import com.moonback.pilgrimage.model.dto.SceneImageRowDto;
import com.moonback.pilgrimage.model.dto.WishlistSceneRowDto;
import com.moonback.pilgrimage.model.dto.response.DramaResponseDto;
import com.moonback.pilgrimage.model.dto.response.WishlistSceneResponseDto;

public interface WishlistMapper {

	int addWishlist(int memberId, int sceneId);

	int countByMemberIdAndSceneId(int memberId, int sceneId);

	void getWishlist(int memberId);

	int deleteWishlist(int memberId, int sceneId);

	List<DramaResponseDto> selectWishlistedDramasByMemberId(int memberId);

	List<WishlistSceneRowDto> selectWishlistedScenesByMemberId(int memberId);

	int countWishlistedScenesByDrama(int memberId, int dramaId);

	List<WishlistSceneRowDto> selectWishlistedScenesRowsByDrama(int memberId, int dramaId, int size, int offset);
}
