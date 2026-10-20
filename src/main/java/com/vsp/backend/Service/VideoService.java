package com.vsp.backend.Service;

import com.vsp.backend.Dto.VideoResponse;
import com.vsp.backend.Model.Like;
import com.vsp.backend.Model.Subscription;
import com.vsp.backend.Model.User;
import com.vsp.backend.Model.Video;
import com.vsp.backend.Repository.CommentRepository;
import com.vsp.backend.Repository.LikeRepository;
import com.vsp.backend.Repository.SubscriptionRepository;
import com.vsp.backend.Repository.UserRepository;
import com.vsp.backend.Repository.VideoRepository;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashSet;
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
	private final SubscriptionRepository subscriptionRepository;
	private final CommentRepository commentRepository;
	private final LikeRepository likeRepository;

	public VideoService(UserRepository userRepository, VideoRepository videoRepository,
			SubscriptionRepository subscriptionRepository, CommentRepository commentRepository,
			LikeRepository likeRepository) {
		this.userRepository = userRepository;
		this.videoRepository = videoRepository;
		this.subscriptionRepository = subscriptionRepository;
		this.commentRepository = commentRepository;
		this.likeRepository = likeRepository;
	}

	private static final Set<String> ALLOWED_EXTENSIONS = Set.of("mp4", "mov", "webm", "mkv");
	private static final int MAX_TITLE_LENGTH = 100;
	private static final int MAX_DESCRIPTION_WORDS = 500;

	public Map<String, Object> create(Video video, Long callerId) {
		if (callerId == null) {
			return Map.of("success", false, "message", "Authentication required");
		}
		if (userRepository.findByIdAndIsActiveTrue(callerId).isEmpty()) {
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
		video.setUserId(callerId);
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

		Optional<User> owner = userRepository.findByIdAndIsActiveTrue(video.getUserId());
		if (owner.isEmpty()) {
			return null;
		}

		if (!isOwner && video.getVisibility() == Video.Visibility.PRIVATE) {
			return null;
		}

		if (!isOwner && owner.get().getVisibility() == User.Visibility.PRIVATE
				&& !subscriptionRepository.existsBySubscriberIdAndChannelIdAndStatusAndIsActiveTrue(viewerId,
						video.getUserId(), Subscription.Status.APPROVED)) {
			return null;
		}

		videoRepository.incrementViewCount(id);
		video.setViewCount(video.getViewCount() + 1);
		return VideoResponse.fullView(video, owner.get());
	}

	public Slice<VideoResponse> getHomeFeed(int page, int size) {
		Pageable pageable = PageRequest.of(page, size, Sort.by(Sort.Direction.DESC, "createdAt"));
		return toCardSlice(videoRepository.findPublicFeed(Video.Visibility.PUBLIC, User.Visibility.PUBLIC, pageable));
	}

	public Slice<VideoResponse> searchByTitle(String title, int page, int size) {
		Pageable pageable = PageRequest.of(page, size, Sort.by(Sort.Direction.DESC, "createdAt"));

		if (title == null || title.isBlank()) {
			return new SliceImpl<>(List.of(), pageable, false);
		}

		return toCardSlice(
				videoRepository.searchPublicByTitle(Video.Visibility.PUBLIC, title.trim(), User.Visibility.PUBLIC, pageable));
	}

	public Slice<VideoResponse> getChannelVideos(Long userId, Long viewerId, int page, int size) {
		return channelVideos(userRepository.findByIdAndIsActiveTrue(userId), viewerId, page, size);
	}

	public Slice<VideoResponse> getChannelVideosByUsername(String username, Long viewerId, int page, int size) {
		Optional<User> channel = username == null || username.isBlank() ? Optional.empty()
				: userRepository.findFirstByUsernameIgnoreCaseAndIsActiveTrue(username.trim());
		return channelVideos(channel, viewerId, page, size);
	}

	private Slice<VideoResponse> channelVideos(Optional<User> channel, Long viewerId, int page, int size) {
		Pageable pageable = PageRequest.of(page, size, Sort.by(Sort.Direction.DESC, "createdAt"));

		if (channel.isEmpty()) {
			return null;
		}

		Long userId = channel.get().getId();
		boolean isOwner = viewerId != null && viewerId.equals(userId);
		if (!isOwner && channel.get().getVisibility() == User.Visibility.PRIVATE) {
			return new SliceImpl<>(List.of(), pageable, false);
		}

		User owner = channel.get();
		if (isOwner) {
			return videoRepository.findByUserIdAndIsActiveTrue(userId, pageable).map(v -> VideoResponse.cardView(v, owner));
		}

		return videoRepository.findByUserIdAndIsActiveTrueAndVisibility(userId, Video.Visibility.PUBLIC, pageable)
				.map(v -> VideoResponse.cardView(v, owner));
	}

	private Slice<VideoResponse> toCardSlice(Slice<Video> slice) {
		Map<Long, User> channels = loadChannels(slice.getContent());
		List<VideoResponse> content = new ArrayList<>();
		for (Video video : slice.getContent()) {
			User channel = channels.get(video.getUserId());
			if (channel == null) {
				continue;
			}
			content.add(VideoResponse.cardView(video, channel));
		}
		return new SliceImpl<>(content, slice.getPageable(), slice.hasNext());
	}

	private Map<Long, User> loadChannels(List<Video> videos) {
		Set<Long> ids = new LinkedHashSet<>();
		for (Video video : videos) {
			if (video.getUserId() != null) {
				ids.add(video.getUserId());
			}
		}
		if (ids.isEmpty()) {
			return Map.of();
		}
		Map<Long, User> channels = new HashMap<>();
		for (User user : userRepository.findAllById(ids)) {
			channels.put(user.getId(), user);
		}
		return channels;
	}

	@Transactional
	public Map<String, Object> updateById(Long id, Video changes, Long callerId) {
		Optional<Video> result = videoRepository.findByIdAndIsActiveTrue(id);

		if (result.isEmpty()) {
			return Map.of("success", false, "message", "Video not found");
		}

		Video video = result.get();

		if (callerId == null || !callerId.equals(video.getUserId())) {
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

	@Transactional
	public Map<String, Object> deleteById(Long id, Long callerId) {
		Optional<Video> result = videoRepository.findByIdAndIsActiveTrue(id);

		if (result.isEmpty()) {
			return Map.of("success", false, "message", "Video not found");
		}

		Video video = result.get();

		if (callerId == null || !callerId.equals(video.getUserId())) {
			return Map.of("success", false, "message", "Owner only");
		}

		video.setIsActive(false);
		videoRepository.save(video);
		commentRepository.deactivateByVideoId(id);
		likeRepository.deactivateByTarget(Like.TargetType.VIDEO, id);
		return Map.of("success", true, "message", "Video deleted");
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
