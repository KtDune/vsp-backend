package com.vsp.backend.Service;

import com.vsp.backend.Dto.PostRequest;
import com.vsp.backend.Dto.PostResponse;
import com.vsp.backend.Model.Like;
import com.vsp.backend.Model.Post;
import com.vsp.backend.Model.PostImage;
import com.vsp.backend.Model.Subscription;
import com.vsp.backend.Model.User;
import com.vsp.backend.Repository.CommentRepository;
import com.vsp.backend.Repository.LikeRepository;
import com.vsp.backend.Repository.PostImageRepository;
import com.vsp.backend.Repository.PostRepository;
import com.vsp.backend.Repository.SubscriptionRepository;
import com.vsp.backend.Repository.UserRepository;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.LinkedHashMap;
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
public class PostService {

	private static final int MAX_CONTENT_LENGTH = 1000;
	private static final int MAX_IMAGES = 4;

	private final UserRepository userRepository;
	private final PostRepository postRepository;
	private final PostImageRepository postImageRepository;
	private final SubscriptionRepository subscriptionRepository;
	private final CommentRepository commentRepository;
	private final LikeRepository likeRepository;
	private final StorageService storageService;

	public PostService(UserRepository userRepository, PostRepository postRepository,
			PostImageRepository postImageRepository, SubscriptionRepository subscriptionRepository,
			CommentRepository commentRepository, LikeRepository likeRepository, StorageService storageService) {
		this.userRepository = userRepository;
		this.postRepository = postRepository;
		this.postImageRepository = postImageRepository;
		this.subscriptionRepository = subscriptionRepository;
		this.commentRepository = commentRepository;
		this.likeRepository = likeRepository;
		this.storageService = storageService;
	}

	@Transactional
	public Map<String, Object> create(PostRequest request, Long callerId) {
		if (callerId == null) {
			return Map.of("success", false, "message", "Authentication required");
		}
		if (userRepository.findByIdAndIsActiveTrue(callerId).isEmpty()) {
			return Map.of("success", false, "message", "User not found or deactivated");
		}

		String content = normalizeContent(request.content());
		if (content != null && content.length() > MAX_CONTENT_LENGTH) {
			return Map.of("success", false, "message", "Text must be 0-1000 characters");
		}

		List<String> imageUrls = normalizeImageUrls(request.imageUrls());
		if (imageUrls == null) {
			return Map.of("success", false, "message", "Image URLs must be non-blank");
		}
		if (imageUrls.size() > MAX_IMAGES) {
			return Map.of("success", false, "message", "A post can have at most " + MAX_IMAGES + " images");
		}
		if (!hasOwnedImages(imageUrls, callerId)) {
			return Map.of("success", false, "message", "Image URLs are invalid or were not uploaded by you");
		}
		if (content == null && imageUrls.isEmpty()) {
			return Map.of("success", false, "message", "At least one of text or image is required");
		}

		Post.Visibility visibility = parseVisibility(request.visibility());
		if (visibility == null) {
			return Map.of("success", false, "message", "Visibility must be PUBLIC or PRIVATE");
		}

		Post post = new Post();
		post.setUserId(callerId);
		post.setContent(content);
		post.setVisibility(visibility);
		post.setIsActive(true);

		Post saved = postRepository.save(post);
		saveImages(saved.getId(), callerId, imageUrls);

		return Map.of("success", true, "message", "Post created", "id", saved.getId());
	}

	@Transactional
	public PostResponse getById(Long id, Long viewerId) {
		Optional<Post> result = postRepository.findByIdAndIsActiveTrue(id);

		if (result.isEmpty()) {
			return null;
		}

		Post post = result.get();
		boolean isOwner = viewerId != null && viewerId.equals(post.getUserId());

		Optional<User> author = userRepository.findByIdAndIsActiveTrue(post.getUserId());
		if (author.isEmpty()) {
			return null;
		}

		if (!isOwner && !canViewContent(viewerId, author.get(), post)) {
			return null;
		}

		return PostResponse.from(post, loadImageUrlsForPost(post.getId()), author.get());
	}

	public Slice<PostResponse> getHomeFeed(int page, int size) {
		Pageable pageable = PageRequest.of(page, size, Sort.by(Sort.Direction.DESC, "createdAt"));
		Slice<Post> slice = postRepository.findPublicFeed(Post.Visibility.PUBLIC, User.Visibility.PUBLIC, pageable);

		Map<Long, User> authors = loadAuthors(slice.getContent());
		Map<Long, List<String>> images = loadImages(slice.getContent());

		List<PostResponse> content = new ArrayList<>();
		for (Post post : slice.getContent()) {
			User author = authors.get(post.getUserId());
			if (author == null) {
				continue;
			}
			content.add(PostResponse.from(post, images.getOrDefault(post.getId(), List.of()), author));
		}
		return new SliceImpl<>(content, slice.getPageable(), slice.hasNext());
	}

	public Slice<PostResponse> getChannelPosts(Long userId, Long viewerId, int page, int size) {
		return channelPosts(userRepository.findByIdAndIsActiveTrue(userId), viewerId, page, size);
	}

	public Slice<PostResponse> getChannelPostsByUsername(String username, Long viewerId, int page, int size) {
		Optional<User> channel = username == null || username.isBlank() ? Optional.empty()
				: userRepository.findFirstByUsernameIgnoreCaseAndIsActiveTrue(username.trim());
		return channelPosts(channel, viewerId, page, size);
	}

	private Slice<PostResponse> channelPosts(Optional<User> channel, Long viewerId, int page, int size) {
		Pageable pageable = PageRequest.of(page, size, Sort.by(Sort.Direction.DESC, "createdAt"));

		if (channel.isEmpty()) {
			return null;
		}

		User owner = channel.get();
		boolean isOwner = viewerId != null && viewerId.equals(owner.getId());

		Slice<Post> posts;
		if (isOwner) {
			posts = postRepository.findByUserIdAndIsActiveTrue(owner.getId(), pageable);
		} else if (!canViewAccountContent(viewerId, owner)) {
			return new SliceImpl<>(List.of(), pageable, false);
		} else {
			posts = postRepository.findByUserIdAndIsActiveTrueAndVisibility(owner.getId(), Post.Visibility.PUBLIC,
					pageable);
		}

		Map<Long, List<String>> images = loadImages(posts.getContent());
		List<PostResponse> content = new ArrayList<>();
		for (Post post : posts.getContent()) {
			content.add(PostResponse.from(post, images.getOrDefault(post.getId(), List.of()), owner));
		}
		return new SliceImpl<>(content, posts.getPageable(), posts.hasNext());
	}

	@Transactional
	public Map<String, Object> updateById(Long id, PostRequest changes, Long callerId) {
		Optional<Post> result = postRepository.findByIdAndIsActiveTrue(id);

		if (result.isEmpty()) {
			return Map.of("success", false, "message", "Post not found");
		}

		Post post = result.get();

		if (callerId == null || !callerId.equals(post.getUserId())) {
			return Map.of("success", false, "message", "Owner only");
		}

		boolean contentProvided = changes.content() != null;
		boolean imagesProvided = changes.imageUrls() != null;
		boolean visibilityProvided = changes.visibility() != null;

		if (!contentProvided && !imagesProvided && !visibilityProvided) {
			return Map.of("success", false, "message", "Nothing to update");
		}

		String newContent = contentProvided ? normalizeContent(changes.content()) : null;
		if (newContent != null && newContent.length() > MAX_CONTENT_LENGTH) {
			return Map.of("success", false, "message", "Text must be 0-1000 characters");
		}

		List<String> newImages = null;
		if (imagesProvided) {
			newImages = normalizeImageUrls(changes.imageUrls());
			if (newImages == null) {
				return Map.of("success", false, "message", "Image URLs must be non-blank");
			}
			if (newImages.size() > MAX_IMAGES) {
				return Map.of("success", false, "message", "A post can have at most " + MAX_IMAGES + " images");
			}
			if (!hasOwnedImages(newImages, callerId)) {
				return Map.of("success", false, "message", "Image URLs are invalid or were not uploaded by you");
			}
		}

		String effectiveContent = contentProvided ? newContent : post.getContent();
		List<String> effectiveImages = imagesProvided ? newImages : loadImageUrlsForPost(post.getId());
		if ((effectiveContent == null || effectiveContent.isEmpty()) && effectiveImages.isEmpty()) {
			return Map.of("success", false, "message", "At least one of text or image is required");
		}

		Post.Visibility visibility = post.getVisibility();
		if (visibilityProvided) {
			visibility = parseVisibility(changes.visibility());
			if (visibility == null) {
				return Map.of("success", false, "message", "Visibility must be PUBLIC or PRIVATE");
			}
		}

		if (contentProvided) {
			post.setContent(newContent);
		}
		post.setVisibility(visibility);
		if (imagesProvided) {
			List<String> oldImages = loadImageUrlsForPost(post.getId());
			postImageRepository.deactivateByPostId(post.getId());
			saveImages(post.getId(), callerId, newImages);
			for (String oldImageUrl : oldImages) {
				if (!newImages.contains(oldImageUrl)) {
					storageService.deleteByUrl(oldImageUrl);
				}
			}
		}

		post.setIsEdited(true);
		postRepository.save(post);
		return Map.of("success", true, "message", "Post updated");
	}

	@Transactional
	public Map<String, Object> deleteById(Long id, Long callerId) {
		Optional<Post> result = postRepository.findByIdAndIsActiveTrue(id);

		if (result.isEmpty()) {
			return Map.of("success", false, "message", "Post not found");
		}

		Post post = result.get();

		if (callerId == null || !callerId.equals(post.getUserId())) {
			return Map.of("success", false, "message", "Owner only");
		}

		post.setIsActive(false);
		postRepository.save(post);
		List<String> imageUrls = loadImageUrlsForPost(id);
		postImageRepository.deactivateByPostId(id);
		commentRepository.deactivateByPostId(id);
		likeRepository.deactivateByTarget(Like.TargetType.POST, id);
		for (String imageUrl : imageUrls) {
			storageService.deleteByUrl(imageUrl);
		}

		return Map.of("success", true, "message", "Post deleted");
	}

	private boolean canViewContent(Long viewerId, User owner, Post post) {
		if (post.getVisibility() == Post.Visibility.PRIVATE) {
			return false;
		}
		return canViewAccountContent(viewerId, owner);
	}

	private boolean canViewAccountContent(Long viewerId, User owner) {
		if (owner.getVisibility() != User.Visibility.PRIVATE) {
			return true;
		}
		return viewerId != null && isApprovedSubscriber(viewerId, owner.getId());
	}

	private boolean isApprovedSubscriber(Long viewerId, Long ownerId) {
		return subscriptionRepository.findBySubscriberIdAndChannelIdAndIsActiveTrue(viewerId, ownerId)
				.map(s -> s.getStatus() == Subscription.Status.APPROVED)
				.orElse(false);
	}

	private Map<Long, User> loadAuthors(Collection<Post> posts) {
		Set<Long> ids = new LinkedHashSet<>();
		for (Post post : posts) {
			if (post.getUserId() != null) {
				ids.add(post.getUserId());
			}
		}
		if (ids.isEmpty()) {
			return Map.of();
		}
		Map<Long, User> authors = new HashMap<>();
		for (User user : userRepository.findAllById(ids)) {
			authors.put(user.getId(), user);
		}
		return authors;
	}

	private Map<Long, List<String>> loadImages(Collection<Post> posts) {
		if (posts.isEmpty()) {
			return Map.of();
		}
		Set<Long> ids = new LinkedHashSet<>();
		for (Post post : posts) {
			ids.add(post.getId());
		}
		Map<Long, List<String>> images = new LinkedHashMap<>();
		for (PostImage image : postImageRepository.findByPostIdInAndIsActiveTrueOrderBySortOrderAscIdAsc(ids)) {
			images.computeIfAbsent(image.getPostId(), k -> new ArrayList<>()).add(image.getImageUrl());
		}
		return images;
	}

	private List<String> loadImageUrlsForPost(Long postId) {
		List<String> urls = new ArrayList<>();
		for (PostImage image : postImageRepository
				.findByPostIdInAndIsActiveTrueOrderBySortOrderAscIdAsc(List.of(postId))) {
			urls.add(image.getImageUrl());
		}
		return urls;
	}

	private void saveImages(Long postId, Long userId, List<String> imageUrls) {
		for (int i = 0; i < imageUrls.size(); i++) {
			PostImage image = new PostImage();
			image.setPostId(postId);
			image.setUserId(userId);
			image.setImageUrl(imageUrls.get(i));
			image.setSortOrder(i);
			image.setIsActive(true);
			postImageRepository.save(image);
		}
	}

	private boolean hasOwnedImages(List<String> imageUrls, Long callerId) {
		for (String imageUrl : imageUrls) {
			if (!storageService.isOwnedMediaUrl(imageUrl, StorageService.MediaKind.IMAGE, callerId)) {
				return false;
			}
		}
		return true;
	}

	private String normalizeContent(String content) {
		if (content == null) {
			return null;
		}
		String trimmed = content.trim();
		return trimmed.isEmpty() ? null : trimmed;
	}

	private List<String> normalizeImageUrls(List<String> imageUrls) {
		if (imageUrls == null) {
			return List.of();
		}
		List<String> normalized = new ArrayList<>();
		for (String url : imageUrls) {
			if (url == null || url.trim().isEmpty()) {
				return null;
			}
			normalized.add(url.trim());
		}
		return normalized;
	}

	private Post.Visibility parseVisibility(String value) {
		if (value == null || value.isBlank()) {
			return Post.Visibility.PUBLIC;
		}
		try {
			return Post.Visibility.valueOf(value.trim().toUpperCase(Locale.ROOT));
		} catch (IllegalArgumentException e) {
			return null;
		}
	}
}
