package com.moonback.pilgrimage.model.service.impl;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.moonback.pilgrimage.exception.BusinessException;
import com.moonback.pilgrimage.exception.code.SceneErrorCode;
import com.moonback.pilgrimage.exception.code.WishlistErrorCode;
import com.moonback.pilgrimage.model.dto.SceneDto;
import com.moonback.pilgrimage.model.dto.response.WishlistResponseDto;
import com.moonback.pilgrimage.model.mapper.SceneMapper;
import com.moonback.pilgrimage.model.mapper.WishlistMapper;
import com.moonback.pilgrimage.model.service.WishlistService;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class WishlistServiceImpl implements WishlistService {
	
	private final SceneMapper sceneMapper;
	private final WishlistMapper wishlistMapper;
	
	@Override
	@Transactional
	public void addWishlist(int memberId, int sceneId) {
		SceneDto scene = sceneMapper.findById(sceneId);

	    if (scene == null) {
	        throw new BusinessException(SceneErrorCode.SCENE_NOT_FOUND);
	    }

	    int count = wishlistMapper.countByMemberIdAndSceneId(memberId, sceneId);

	    if (count > 0) {
	        throw new BusinessException(WishlistErrorCode.WISHLIST_ALREADY_EXISTS);
	    }

	    wishlistMapper.addWishlist(memberId, sceneId);
	}

	@Override
	public WishlistResponseDto getWishlist(int memberId) {
		wishlistMapper.getWishlist(memberId);
		return null;
	}

	@Override
	public void deleteWishlist(int memberId, int sceneId) {
		wishlistMapper.deleteWishlist(memberId, sceneId);
		
	}

}
