package com.vsp.backend.Controller;

import java.util.Map;

import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

import com.vsp.backend.Security.AppUserPrincipal;
import com.vsp.backend.Service.StorageService;

@RestController
@RequestMapping("/api/v1/uploads")
public class StorageController {

	private final StorageService storageService;

	public StorageController(StorageService storageService) {
		this.storageService = storageService;
	}

	@PostMapping("/videos")
	public ResponseEntity<Map<String, Object>> uploadVideo(@RequestParam("file") MultipartFile file,
			@AuthenticationPrincipal AppUserPrincipal me) {
		return respond(storageService.upload(file, StorageService.MediaKind.VIDEO, me.id()));
	}

	@PostMapping("/images")
	public ResponseEntity<Map<String, Object>> uploadImage(@RequestParam("file") MultipartFile file,
			@AuthenticationPrincipal AppUserPrincipal me) {
		return respond(storageService.upload(file, StorageService.MediaKind.IMAGE, me.id()));
	}

	@PostMapping("/avatars")
	public ResponseEntity<Map<String, Object>> uploadAvatar(@RequestParam("file") MultipartFile file,
			@AuthenticationPrincipal AppUserPrincipal me) {
		return respond(storageService.upload(file, StorageService.MediaKind.AVATAR, me.id()));
	}

	private ResponseEntity<Map<String, Object>> respond(Map<String, Object> result) {
		return result.get("success").equals(true)
				? ResponseEntity.ok(result)
				: ResponseEntity.badRequest().body(result);
	}
}
