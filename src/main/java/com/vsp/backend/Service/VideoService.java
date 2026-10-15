package com.vsp.backend.Service;

import com.vsp.backend.Dto.VideoResponse;
import com.vsp.backend.Model.User;
import com.vsp.backend.Model.Video;
import com.vsp.backend.Repository.UserRepository;
import com.vsp.backend.Repository.VideoRepository;

import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;
import java.util.Set;

import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Slice;
import org.springframework.data.domain.SliceImpl;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class VideoService {

	private final UserRepository userRepository;
	private final VideoRepository videoRepository;

	public VideoService(UserRepository userRepository, VideoRepository videoRepository) {
		this.userRepository = userRepository;
		this.videoRepository = videoRepository;
	}

	private static final Set<String> ALLOWED_EXTENSIONS = Set.of("mp4", "mov", "webm", "mkv");
	private static final int MAX_TITLE_LENGTH = 100;
	private static final int MAX_DESCRIPTION_WORDS = 500;

	public Map<String, Object> create(Video video) {
		if (video.getUserId() == null) {
			return Map.of("success", false, "message", "User is required");
		}
		if (userRepository.findByIdAndIsActiveTrue(video.getUserId()).isEmpty()) {
			return Map.of("success", false, "message", "User not found or deactivated");
		}
		if (!isValidTitle(video.getTitle())) {
			return Map.of("success", false, "message", "Title is required (1-100 characters)");
		}
		if (!isValidDescription(video.getDescription())) {
			return Map.of("success", false, "message", "Description must be empty or up to 500 words");
		}
		if (!hasAllowedExtension(video.getVideoUrl())) {
			return Map.of("success", false, "message", "Video file is required (mp4, mov, webm, or mkv)");
		}

		video.setId(null);
		if (video.getVisibility() == null) {
			video.setVisibility(Video.Visibility.PUBLIC);
		}
		if (video.getViewCount() == null) {
			video.setViewCount(0);
		}
		if (video.getCommentsDisabled() == null) {
			video.setCommentsDisabled(false);
		}
		video.setIsActive(true);

		Video saved = videoRepository.save(video);
		return Map.of("success", true, "message", "Video created", "id", saved.getId());
	}

	@Transactional
	public VideoResponse getById(Long id, Long viewerId) {
		Optional<Video> result = videoRepository.findByIdAndIsActiveTrue(id);

		if (result.isEmpty()) {
			return null;
		}

		Video video = result.get();
		boolean isOwner = viewerId != null && viewerId.equals(video.getUserId());

		if (!isOwner && video.getVisibility() == Video.Visibility.PRIVATE) {
			return null;
		}

		if (!isOwner) {
			Optional<User> owner = userRepository.findByIdAndIsActiveTrue(video.getUserId());
			if (owner.isEmpty() || owner.get().getVisibility() == User.Visibility.PRIVATE) {
				return null;
			}
		}

		videoRepository.incrementViewCount(id);
		video.setViewCount(video.getViewCount() + 1);
		return VideoResponse.fullView(video);
	}

	public Slice<Video> getHomeFeed(int page, int size) {
		Pageable pageable = PageRequest.of(page, size, Sort.by(Sort.Direction.DESC, "createdAt"));
		return videoRepository.findPublicFeed(Video.Visibility.PUBLIC, User.Visibility.PUBLIC, pageable);
	}

	public Slice<Video> getChannelVideos(Long userId, Long viewerId, int page, int size) {
		Pageable pageable = PageRequest.of(page, size, Sort.by(Sort.Direction.DESC, "createdAt"));

		Optional<User> channel = userRepository.findByIdAndIsActiveTrue(userId);
		if (channel.isEmpty()) {
			return null;
		}

		boolean isOwner = viewerId != null && viewerId.equals(userId);
		if (!isOwner && channel.get().getVisibility() == User.Visibility.PRIVATE) {
			return new SliceImpl<>(List.of(), pageable, false);
		}

		if (isOwner) {
			return videoRepository.findByUserIdAndIsActiveTrue(userId, pageable);
		}

		return videoRepository.findByUserIdAndIsActiveTrueAndVisibility(userId, Video.Visibility.PUBLIC, pageable);
	}

	@Transactional
	public Map<String, Object> updateById(Long id, Video changes) {
		Optional<Video> result = videoRepository.findByIdAndIsActiveTrue(id);

		if (result.isEmpty()) {
			return Map.of("success", false, "message", "Video not found");
		}

		Video video = result.get();

		if (changes.getUserId() == null || !changes.getUserId().equals(video.getUserId())) {
			return Map.of("success", false, "message", "Owner only");
		}

		if (changes.getTitle() != null && !isValidTitle(changes.getTitle())) {
			return Map.of("success", false, "message", "Title must be 1-100 characters");
		}

		if (changes.getDescription() != null && !isValidDescription(changes.getDescription())) {
			return Map.of("success", false, "message", "Description must be empty or up to 500 words");
		}

		if (changes.getTitle() != null) {
			video.setTitle(changes.getTitle());
		}
		if (changes.getDescription() != null) {
			video.setDescription(changes.getDescription());
		}
		if (changes.getVisibility() != null) {
			video.setVisibility(changes.getVisibility());
		}
		if (changes.getThumbnailUrl() != null) {
			video.setThumbnailUrl(changes.getThumbnailUrl());
		}
		if (changes.getCommentsDisabled() != null) {
			video.setCommentsDisabled(changes.getCommentsDisabled());
		}

		videoRepository.save(video);
		return Map.of("success", true, "message", "Video updated");
	}

	private boolean isValidTitle(String title) {
		return title != null && !title.isBlank() && title.length() <= MAX_TITLE_LENGTH;
	}

	private boolean isValidDescription(String description) {
		if (description == null) {
			return true;
		}
		String trimmed = description.trim();
		if (trimmed.isEmpty()) {
			return true;
		}
		return trimmed.split("\\s+").length <= MAX_DESCRIPTION_WORDS;
	}

	private boolean hasAllowedExtension(String videoUrl) {
		if (videoUrl == null || videoUrl.isBlank()) {
			return false;
		}
		int dot = videoUrl.lastIndexOf('.');
		if (dot < 0 || dot == videoUrl.length() - 1) {
			return false;
		}
		String extension = videoUrl.substring(dot + 1).toLowerCase(Locale.ROOT);
		return ALLOWED_EXTENSIONS.contains(extension);
	}
}
