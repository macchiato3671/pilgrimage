package com.moonback.pilgrimage.model.service.impl;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.moonback.pilgrimage.exception.BusinessException;
import com.moonback.pilgrimage.exception.code.DramaErrorCode;
import com.moonback.pilgrimage.exception.code.SceneErrorCode;
import com.moonback.pilgrimage.exception.code.WishlistErrorCode;
import com.moonback.pilgrimage.model.dto.DramaGenreRowDto;
import com.moonback.pilgrimage.model.dto.DramaImageDto;
import com.moonback.pilgrimage.model.dto.DramaImageRowDto;
import com.moonback.pilgrimage.model.dto.GenreDto;
import com.moonback.pilgrimage.model.dto.SceneDto;
import com.moonback.pilgrimage.model.dto.SceneImageDto;
import com.moonback.pilgrimage.model.dto.SceneImageRowDto;
import com.moonback.pilgrimage.model.dto.WishlistSceneRowDto;
import com.moonback.pilgrimage.model.dto.response.DramaResponseDto;
import com.moonback.pilgrimage.model.dto.response.PageResponseDto;
import com.moonback.pilgrimage.model.dto.response.WishlistDramaResponseDto;
import com.moonback.pilgrimage.model.dto.response.WishlistScenePageResponseDto;
import com.moonback.pilgrimage.model.dto.response.WishlistSceneResponseDto;
import com.moonback.pilgrimage.model.mapper.DramaMapper;
import com.moonback.pilgrimage.model.mapper.SceneMapper;
import com.moonback.pilgrimage.model.mapper.WishlistMapper;
import com.moonback.pilgrimage.model.service.S3PresignedUrlService;
import com.moonback.pilgrimage.model.service.WishlistService;
import com.moonback.pilgrimage.validator.MemberValidator;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class WishlistServiceImpl implements WishlistService {
	
	private static final int MAX_PAGE_SIZE = 50;
	
	private final SceneMapper sceneMapper;
	private final WishlistMapper wishlistMapper;
	private final DramaMapper dramaMapper;
	private final MemberValidator memberValidator;
	private final S3PresignedUrlService s3PresignedUrlService;
	
	@Override
	@Transactional
	public void addWishlist(int memberId, int sceneId) {
		memberValidator.validateActiveMember(memberId);
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
	@Transactional
	public void deleteWishlist(int memberId, int sceneId) {
		memberValidator.validateActiveMember(memberId);
		
		int deleteCount = wishlistMapper.deleteWishlist(memberId, sceneId);
		
		if(deleteCount == 0) {
			throw new BusinessException(WishlistErrorCode.WISHLIST_NOT_FOUND);
		}
	}

	@Override
	@Transactional(readOnly = true)
	public WishlistDramaResponseDto getDrama(int memberId) {
		memberValidator.validateActiveMember(memberId);
		
		List<DramaResponseDto> dramas = 
				wishlistMapper.selectWishlistedDramasByMemberId(memberId);
		
		if(dramas.isEmpty()) {
			return WishlistDramaResponseDto.builder()
					.dramas(dramas)
					.build();
		}
		
		List<Integer> dramaIds = dramas.stream()
				.map(DramaResponseDto::getDramaId)
				.toList();
		
		List<DramaGenreRowDto> genreRows = 
				dramaMapper.selectGenresByDramaIds(dramaIds);
		
		List<DramaImageRowDto> imageRows =
				dramaMapper.selectDramaImagesByDramaIds(dramaIds);
		
		Map<Integer, List<GenreDto>> genreMap = genreRows.stream()
				.collect(Collectors.groupingBy(
						DramaGenreRowDto::getDramaId,
						Collectors.mapping(
								row -> GenreDto.builder()
										.genreId(row.getGenreId())
										.name(row.getName())
										.build(),
								Collectors.toList()
						)
				));
		
		Map<Integer, List<DramaImageDto>> imageMap = imageRows.stream()
				.collect(Collectors.groupingBy(
						DramaImageRowDto::getDramaId,
						Collectors.mapping(
								this::toFetchableDramaImage,
								Collectors.toList()
						)
				));
		
		dramas.forEach(drama ->
				drama.setGenres(
						genreMap.getOrDefault(drama.getDramaId(), List.of())
				)
		);
		
		dramas.forEach(drama ->
				drama.setImages(
						imageMap.getOrDefault(drama.getDramaId(), List.of())
				)
		);
		
		return WishlistDramaResponseDto.builder()
				.dramas(dramas)
				.build();
	}
	
	@Override
	@Transactional(readOnly = true)
	public WishlistScenePageResponseDto getScene(int memberId, int dramaId, int page, int size) {
		memberValidator.validateActiveMember(memberId);
		validatePageRequest(page, size);
		
		boolean existsDrama = dramaMapper.existsByDramaId(dramaId);
		if(!existsDrama) {
			throw new BusinessException(DramaErrorCode.DRAMA_NOT_FOUND);
		}
		
		int totalElements = wishlistMapper.countWishlistedScenesByDrama(memberId, dramaId);
		int totalPages = calculateTotalPages(totalElements, size);
		
		int offset = page * size;
		
		List<WishlistSceneRowDto> sceneRows = 
				wishlistMapper.selectWishlistedScenesRowsByDrama(
						memberId,
						dramaId,
						size,
						offset
				);
		
		if (sceneRows.isEmpty()) {
            return WishlistScenePageResponseDto.builder()
                    .scenes(List.of())
                    .page(createPageResponse(page, size, totalElements, totalPages))
                    .build();
        }
		
		List<Integer> sceneIds = sceneRows.stream()
				.map(WishlistSceneRowDto::getSceneId)
				.toList();
		
		List<SceneImageRowDto> imageRows = sceneMapper.selectSceneImagesBySceneIds(sceneIds);
		
		Map<Integer, List<SceneImageDto>> imageMap = imageRows.stream()
                .collect(Collectors.groupingBy(
                        SceneImageRowDto::getSceneId,
                        Collectors.mapping(
                                this::toFetchableSceneImage,
                                Collectors.toList()
                        )
                ));

        List<WishlistSceneResponseDto> scenes = sceneRows.stream()
                .map(scene -> WishlistSceneResponseDto.builder()
                        .wishlistId(scene.getWishlistId())
                        .sceneId(scene.getSceneId())
                        .name(scene.getName())
                        .description(scene.getDescription())
                        .address(scene.getAddress())
                        .latitude(scene.getLatitude())
                        .longitude(scene.getLongitude())
                        .createdAt(scene.getCreatedAt())
                        .images(imageMap.getOrDefault(scene.getSceneId(), List.of()))
                        .build())
                .toList();

        return WishlistScenePageResponseDto.builder()
                .scenes(scenes)
                .page(createPageResponse(page, size, totalElements, totalPages))
                .build();
	}
	
	private void validatePageRequest(int page, int size) {
        if (page < 0 || size <= 0 || size > MAX_PAGE_SIZE) {
            throw new BusinessException(WishlistErrorCode.INVALID_PAGE_REQUEST);
        }
    }

    private int calculateTotalPages(int totalElements, int size) {
        if (totalElements == 0) {
            return 0;
        }

        return (int) Math.ceil((double) totalElements / size);
    }
    
    private PageResponseDto createPageResponse(
            int page,
            int size,
            int totalElements,
            int totalPages
    ) {
        return PageResponseDto.builder()
                .number(page)
                .size(size)
                .totalElements(totalElements)
                .totalPages(totalPages)
                .hasNext(page + 1 < totalPages)
                .hasPrevious(page > 0)
                .build();
    }

	private DramaImageDto toFetchableDramaImage(DramaImageRowDto image) {
		return DramaImageDto.builder()
				.imgId(image.getImgId())
				.url(s3PresignedUrlService.toPresignedGetUrl(image.getUrl()))
				.build();
	}

	private SceneImageDto toFetchableSceneImage(SceneImageRowDto image) {
		return SceneImageDto.builder()
				.imgId(image.getImgId())
				.url(s3PresignedUrlService.toPresignedGetUrl(image.getUrl()))
				.build();
	}

}
