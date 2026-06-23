package com.moonback.pilgrimage.controller;

import org.springframework.http.HttpStatus;
import org.springframework.http.HttpStatusCode;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.moonback.pilgrimage.model.dto.response.WishlistDramaResponseDto;
import com.moonback.pilgrimage.model.dto.response.WishlistScenePageResponseDto;
import com.moonback.pilgrimage.model.service.WishlistService;

import lombok.RequiredArgsConstructor;

@RestController
@RequestMapping("/api/v1/wishlist")
@RequiredArgsConstructor
public class WishlistController {
	
	private final WishlistService wishlistService;
	
	@PostMapping("/{sceneId}")
	public ResponseEntity<Void> addWishlist(@PathVariable int sceneId){
		Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
		int memberId = (int)authentication.getPrincipal();
		
		wishlistService.addWishlist(memberId, sceneId);
		
		return ResponseEntity
				.status(HttpStatus.CREATED)
				.build();
	}
	
	@GetMapping("/dramas")
	public ResponseEntity<WishlistDramaResponseDto> getDrama(){
		Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
		int memberId = (int)authentication.getPrincipal();
		
		WishlistDramaResponseDto response = wishlistService.getDrama(memberId);
		
		return ResponseEntity
				.status(HttpStatus.OK)
				.body(response);
	}
	
	@GetMapping("/dramas/{dramaId}/scenes")
	public ResponseEntity<WishlistScenePageResponseDto> getScene(
														@PathVariable int dramaId,
														@RequestParam int page,
														@RequestParam int size){
		Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
		int memberId = (int)authentication.getPrincipal();
		
		WishlistScenePageResponseDto response = wishlistService.getScene(memberId, dramaId, page, size);
		
		return ResponseEntity
				.status(HttpStatus.OK)
				.body(response);
	}
	
	@DeleteMapping("/{sceneId}")
	public ResponseEntity<Void> deleteWishlist(@PathVariable int sceneId){
		Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
		int memberId = (int)authentication.getPrincipal();
		
		wishlistService.deleteWishlist(memberId, sceneId);
		
		return ResponseEntity
				.status(HttpStatus.OK)
				.build();
	}
}
