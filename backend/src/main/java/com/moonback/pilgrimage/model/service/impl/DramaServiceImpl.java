package com.moonback.pilgrimage.model.service.impl;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.moonback.pilgrimage.exception.BusinessException;
import com.moonback.pilgrimage.exception.code.DramaErrorCode;
import com.moonback.pilgrimage.exception.code.WishlistErrorCode;
import com.moonback.pilgrimage.model.dto.DramaDto;
import com.moonback.pilgrimage.model.dto.DramaGenreRowDto;
import com.moonback.pilgrimage.model.dto.DramaImageDto;
import com.moonback.pilgrimage.model.dto.DramaImageRowDto;
import com.moonback.pilgrimage.model.dto.GenreDto;
import com.moonback.pilgrimage.model.dto.SceneDto;
import com.moonback.pilgrimage.model.dto.SceneImageDto;
import com.moonback.pilgrimage.model.dto.SceneImageRowDto;
import com.moonback.pilgrimage.model.dto.response.DramaResponseDto;
import com.moonback.pilgrimage.model.dto.response.DramaSceneResponseDto;
import com.moonback.pilgrimage.model.dto.response.GenreDramaResponseDto;
import com.moonback.pilgrimage.model.dto.response.GenreResponseDto;
import com.moonback.pilgrimage.model.dto.response.KeywordDramaResponseDto;
import com.moonback.pilgrimage.model.dto.response.PageResponseDto;
import com.moonback.pilgrimage.model.dto.response.SceneResponseDto;
import com.moonback.pilgrimage.model.dto.response.YearDramaResponseDto;
import com.moonback.pilgrimage.model.dto.response.YearResponseDto;
import com.moonback.pilgrimage.model.mapper.DramaMapper;
import com.moonback.pilgrimage.model.mapper.SceneMapper;
import com.moonback.pilgrimage.model.service.DramaService;

import lombok.RequiredArgsConstructor;

@Service
@Transactional(readOnly = true)
@RequiredArgsConstructor
public class DramaServiceImpl implements DramaService {
	
	private static final int MAX_PAGE_SIZE = 50;
	private final DramaMapper dramaMapper;
	private final SceneMapper sceneMapper;
	
	@Override
	public DramaSceneResponseDto getScene(int dramaId, int page, int size) {
		validatePageRequest(page, size);
		
		DramaDto existsDrama = dramaMapper.findByDramaId(dramaId);
		if(existsDrama == null) {
			throw new BusinessException(DramaErrorCode.DRAMA_NOT_FOUND);
		}
		
		int totalElements = sceneMapper.countSceneByDramaId(dramaId);
		int totalPages = calculateTotalPages(totalElements, size);
		
		int offset = page * size;
		
		List<SceneDto> sceneRows = 
				sceneMapper.getSceneByDramaId(
						dramaId,
						size,
						offset
				);
		
		if (sceneRows.isEmpty()) {
            return DramaSceneResponseDto.builder()
            		.dramaId(dramaId)
            		.title(existsDrama.getTitle())
                    .scenes(List.of())
                    .page(createPageResponse(page, size, totalElements, totalPages))
                    .build();
        }
		
		List<Integer> sceneIds = sceneRows.stream()
				.map(SceneDto::getSceneId)
				.toList();
		
		List<SceneImageRowDto> imageRows = sceneMapper.selectSceneImagesBySceneIds(sceneIds);
		
		Map<Integer, List<SceneImageDto>> imageMap = imageRows.stream()
                .collect(Collectors.groupingBy(
                        SceneImageRowDto::getSceneId,
                        Collectors.mapping(
                                image -> SceneImageDto.builder()
                                        .imgId(image.getImgId())
                                        .url(image.getUrl())
                                        .build(),
                                Collectors.toList()
                        )
                ));

        List<SceneResponseDto> scenes = sceneRows.stream()
                .map(scene -> SceneResponseDto.builder()
                        .sceneId(scene.getSceneId())
                        .name(scene.getName())
                        .description(scene.getDescription())
                        .address(scene.getAddress())
                        .latitude(scene.getLatitude())
                        .longitude(scene.getLongitude())
                        .images(imageMap.getOrDefault(scene.getSceneId(), List.of()))
                        .build())
                .toList();

        return DramaSceneResponseDto.builder()
        		.dramaId(dramaId)
        		.title(existsDrama.getTitle())
                .scenes(scenes)
                .page(createPageResponse(page, size, totalElements, totalPages))
                .build();
	}

	@Override
	public YearResponseDto getYears() {
		List<Integer> years = dramaMapper.getYears();
		
		return YearResponseDto.builder()
					.years(years)
					.build();
	}

	@Override
	public YearDramaResponseDto getYearDrama(int year, int page, int size) {
		validatePageRequest(page, size);
		
		int totalElements = dramaMapper.countDramasByYear(year);
		int totalPages = calculateTotalPages(totalElements, size);
		
		int offset = page * size;
		
		List<DramaDto> dramaRows = 
				dramaMapper.selectDramasByYear(
						year,
						size,
						offset
				);
		
		if (dramaRows.isEmpty()) {
            return YearDramaResponseDto.builder()
            		.year(year)
                    .dramas(List.of())
                    .page(createPageResponse(page, size, totalElements, totalPages))
                    .build();
        }
		
		List<Integer> dramaIds = dramaRows.stream()
				.map(DramaDto::getDramaId)
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
								image -> DramaImageDto.builder()
										.imgId(image.getImgId())
										.url(image.getUrl())
										.build(),
								Collectors.toList()
						)
				));

		List<DramaResponseDto> dramas = dramaRows.stream()
	            .map(drama -> DramaResponseDto.builder()
	                    .dramaId(drama.getDramaId())
	                    .title(drama.getTitle())
	                    .releasedAt(drama.getReleasedAt())
	                    .description(drama.getDescription())
	                    .images(imageMap.getOrDefault(drama.getDramaId(), List.of()))
	                    .genres(genreMap.getOrDefault(drama.getDramaId(), List.of()))
	                    .build())
	            .toList();
		
        return YearDramaResponseDto.builder()
        		.year(year)
        		.dramas(dramas)
                .page(createPageResponse(page, size, totalElements, totalPages))
                .build();
	}

	@Override
	public GenreResponseDto getGenres() {
		List<GenreDto> genres = dramaMapper.getGenres();
		
		return GenreResponseDto.builder()
					.genres(genres)
					.build();
	}

	@Override
	public GenreDramaResponseDto getGenreDrama(int genreId, int page, int size) {
		validatePageRequest(page, size);
		
		GenreDto genre = dramaMapper.findGenreById(genreId);
		
		int totalElements = dramaMapper.countDramasByGenre(genreId);
		int totalPages = calculateTotalPages(totalElements, size);
		
		int offset = page * size;
		
		List<DramaDto> dramaRows = 
				dramaMapper.selectDramasByGenre(
						genreId,
						size,
						offset
				);
		
		if (dramaRows.isEmpty()) {
            return GenreDramaResponseDto.builder()
            		.genreId(genreId)
            		.name(genre != null ? genre.getName() : null)
                    .dramas(List.of())
                    .page(createPageResponse(page, size, totalElements, totalPages))
                    .build();
        }
		
		List<Integer> dramaIds = dramaRows.stream()
				.map(DramaDto::getDramaId)
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
								image -> DramaImageDto.builder()
										.imgId(image.getImgId())
										.url(image.getUrl())
										.build(),
								Collectors.toList()
						)
				));

		List<DramaResponseDto> dramas = dramaRows.stream()
	            .map(drama -> DramaResponseDto.builder()
	                    .dramaId(drama.getDramaId())
	                    .title(drama.getTitle())
	                    .releasedAt(drama.getReleasedAt())
	                    .description(drama.getDescription())
	                    .images(imageMap.getOrDefault(drama.getDramaId(), List.of()))
	                    .genres(genreMap.getOrDefault(drama.getDramaId(), List.of()))
	                    .build())
	            .toList();

        return GenreDramaResponseDto.builder()
        		.genreId(genreId)
        		.name(genre != null ? genre.getName() : null)
                .dramas(dramas)
                .page(createPageResponse(page, size, totalElements, totalPages))
                .build();
	}

	@Override
	public KeywordDramaResponseDto searchDrama(String keyword, int page, int size) {
		validatePageRequest(page, size);
		
		int totalElements = dramaMapper.countDramasByKeyword(keyword);
		int totalPages = calculateTotalPages(totalElements, size);
		
		int offset = page * size;
		
		List<DramaDto> dramaRows = 
				dramaMapper.selectDramasByKeyword(
						keyword,
						size,
						offset
				);
		
		if (dramaRows.isEmpty()) {
            return KeywordDramaResponseDto.builder()
            		.keyword(keyword)
                    .dramas(List.of())
                    .page(createPageResponse(page, size, totalElements, totalPages))
                    .build();
        }
		
		List<Integer> dramaIds = dramaRows.stream()
				.map(DramaDto::getDramaId)
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
								image -> DramaImageDto.builder()
										.imgId(image.getImgId())
										.url(image.getUrl())
										.build(),
								Collectors.toList()
						)
				));

		List<DramaResponseDto> dramas = dramaRows.stream()
	            .map(drama -> DramaResponseDto.builder()
	                    .dramaId(drama.getDramaId())
	                    .title(drama.getTitle())
	                    .releasedAt(drama.getReleasedAt())
	                    .description(drama.getDescription())
	                    .images(imageMap.getOrDefault(drama.getDramaId(), List.of()))
	                    .genres(genreMap.getOrDefault(drama.getDramaId(), List.of()))
	                    .build())
	            .toList();
		
        return KeywordDramaResponseDto.builder()
        		.keyword(keyword)
                .dramas(dramas)
                .page(createPageResponse(page, size, totalElements, totalPages))
                .build();
	}

	
	private void validatePageRequest(int page, int size) {
        if (page < 0 || size <= 0 || size > MAX_PAGE_SIZE) {
            throw new BusinessException(DramaErrorCode.INVALID_PAGE_REQUEST);
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
}
