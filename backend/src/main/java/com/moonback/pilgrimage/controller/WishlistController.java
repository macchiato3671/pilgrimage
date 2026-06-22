package com.moonback.pilgrimage.controller;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.moonback.pilgrimage.model.service.WishlistService;

import lombok.RequiredArgsConstructor;

@RestController
@RequestMapping("/api/v1/wishlist")
@RequiredArgsConstructor
public class WishlistController {
	
	private final WishlistService wishlistService;
	
	@PostMapping("/{sceneId}")
	public ResponseEntity<Void> addWishlist(@PathVariable int sceneId){
		
		
		wishlistService.addWishlist(memberId, sceneId);
		
		return ResponseEntity
				.status(HttpStatus.CREATED)
				.build();
	}
}
