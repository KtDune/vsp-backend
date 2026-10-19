package com.vsp.backend.Service;

import com.vsp.backend.Dto.CommentRequest;
import com.vsp.backend.Dto.CommentResponse;
import com.vsp.backend.Model.Comment;
import com.vsp.backend.Model.Post;
import com.vsp.backend.Model.Subscription;
import com.vsp.backend.Model.User;
import com.vsp.backend.Model.Video;
import com.vsp.backend.Repository.CommentRepository;
import com.vsp.backend.Repository.PostRepository;
import com.vsp.backend.Repository.SubscriptionRepository;
import com.vsp.backend.Repository.UserRepository;
import com.vsp.backend.Repository.VideoRepository;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.LinkedHashSet;
import java.util.List;
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
public class CommentService {

	private static final int MAX_CONTENT_LENGTH = 500;

	private final CommentRepository commentRepository;
	private final UserRepository userRepository;
	private final VideoRepository videoRepository;
	private final PostRepository postRepository;
	private final SubscriptionRepository subscriptionRepository;

	public CommentService(CommentRepository commentRepository, UserRepository userRepository,
			VideoRepository videoRepository, PostRepository postRepository,
			SubscriptionRepository subscriptionRepository) {
		this.commentRepository = commentRepository;
		this.userRepository = userRepository;
		this.videoRepository = videoRepository;
		this.postRepository = postRepository;
		this.subscriptionRepository = subscriptionRepository;
	}

	@Transactional
	public Map<String, Object> create(CommentRequest request, Long callerId) {
		if (callerId == null) {
			return Map.of("success", false, "message", "Authentication required");
		}
		if (userRepository.findByIdAndIsActiveTrue(callerId).isEmpty()) {
			return Map.of("success", false, "message", "User not found or deactivated");
		}

		String content = normalizeContent(request.content());
		if (content == null) {
			return Map.of("success", false, "message", "Content is required (1-500 characters)");
		}
		if (content.length() > MAX_CONTENT_LENGTH) {
			return Map.of("success", false, "message", "Content must be 1-500 characters");
		}

		if (request.parentId() != null) {
			return createReply(request.parentId(), content, callerId);
		}

		boolean hasVideoId = request.videoId() != null;
		boolean hasPostId = request.postId() != null;
		if (hasVideoId == hasPostId) {
			return Map.of("success", false, "message", "Exactly one of videoId or postId is required");
		}

		if (hasVideoId) {
			Optional<Video> video = videoRepository.findByIdAndIsActiveTrue(request.videoId());
			if (video.isEmpty() || !canViewVideo(callerId, video.get())) {
				return Map.of("success", false, "message", "Video not found or private");
			}
			if (Boolean.TRUE.equals(video.get().getCommentsDisabled())) {
				return Map.of("success", false, "message", "Comments are disabled on this video");
			}
		} else {
			Optional<Post> post = postRepository.findByIdAndIsActiveTrue(request.postId());
			if (post.isEmpty() || !canViewPost(callerId, post.get())) {
				return Map.of("success", false, "message", "Post not found or private");
			}
		}

		Comment comment = new Comment();
		comment.setUserId(callerId);
		comment.setVideoId(hasVideoId ? request.videoId() : null);
		comment.setPostId(hasPostId ? request.postId() : null);
		comment.setContent(content);
		comment.setIsActive(true);

		Comment saved = commentRepository.save(comment);
		return Map.of("success", true, "message", "Comment created", "id", saved.getId());
	}

	@Transactional
	public Map<String, Object> createReply(Long parentId, String rawContent, Long callerId) {
		if (callerId == null) {
			return Map.of("success", false, "message", "Authentication required");
		}

		String content = normalizeContent(rawContent);
		if (content == null) {
			return Map.of("success", false, "message", "Content is required (1-500 characters)");
		}
		if (content.length() > MAX_CONTENT_LENGTH) {
			return Map.of("success", false, "message", "Content must be 1-500 characters");
		}

		Optional<Comment> parent = commentRepository.findByIdAndIsActiveTrue(parentId);
		if (parent.isEmpty() || !canViewTarget(callerId, parent.get())) {
			return Map.of("success", false, "message", "Parent comment not found");
		}

		Comment parentComment = parent.get();
		if (commentsDisabledForTarget(parentComment)) {
			return Map.of("success", false, "message", "Comments are disabled on this video");
		}

		Comment reply = new Comment();
		reply.setUserId(callerId);
		reply.setVideoId(parentComment.getVideoId());
		reply.setPostId(parentComment.getPostId());
		reply.setParentId(parentComment.getId());
		reply.setContent(content);
		reply.setIsActive(true);

		Comment saved = commentRepository.save(reply);
		return Map.of("success", true, "message", "Reply created", "id", saved.getId());
	}

	public Slice<CommentResponse> listForVideo(Long videoId, Long viewerId, int page, int size) {
		Optional<Video> video = videoRepository.findByIdAndIsActiveTrue(videoId);
		if (video.isEmpty() || !canViewVideo(viewerId, video.get())) {
			return null;
		}

		Pageable pageable = PageRequest.of(page, size, Sort.by(Sort.Direction.DESC, "createdAt"));
		Slice<Comment> slice = commentRepository.findByVideoIdAndIsActiveTrueAndParentIdIsNull(videoId, pageable);
		return toResponseSlice(slice);
	}

	public Slice<CommentResponse> listForPost(Long postId, Long viewerId, int page, int size) {
		Optional<Post> post = postRepository.findByIdAndIsActiveTrue(postId);
		if (post.isEmpty() || !canViewPost(viewerId, post.get())) {
			return null;
		}

		Pageable pageable = PageRequest.of(page, size, Sort.by(Sort.Direction.DESC, "createdAt"));
		Slice<Comment> slice = commentRepository.findByPostIdAndIsActiveTrueAndParentIdIsNull(postId, pageable);
		return toResponseSlice(slice);
	}

	public Slice<CommentResponse> listReplies(Long commentId, Long viewerId, int page, int size) {
		Optional<Comment> parent = commentRepository.findByIdAndIsActiveTrue(commentId);
		if (parent.isEmpty() || !canViewTarget(viewerId, parent.get())) {
			return null;
		}

		Pageable pageable = PageRequest.of(page, size, Sort.by(Sort.Direction.ASC, "createdAt", "id"));
		Slice<Comment> slice = commentRepository.findByParentIdAndIsActiveTrue(commentId, pageable);
		return toResponseSlice(slice);
	}

	@Transactional
	public Map<String, Object> updateById(Long id, CommentRequest changes, Long callerId) {
		Optional<Comment> result = commentRepository.findByIdAndIsActiveTrue(id);

		if (result.isEmpty()) {
			return Map.of("success", false, "message", "Comment not found");
		}

		Comment comment = result.get();

		if (callerId == null || !callerId.equals(comment.getUserId())) {
			return Map.of("success", false, "message", "Author only");
		}

		String content = normalizeContent(changes.content());
		if (content == null) {
			return Map.of("success", false, "message", "Content is required (1-500 characters)");
		}
		if (content.length() > MAX_CONTENT_LENGTH) {
			return Map.of("success", false, "message", "Content must be 1-500 characters");
		}

		comment.setContent(content);
		comment.setIsEdited(true);
		commentRepository.save(comment);
		return Map.of("success", true, "message", "Comment updated");
	}

	@Transactional
	public Map<String, Object> deleteById(Long id, Long callerId) {
		Optional<Comment> result = commentRepository.findByIdAndIsActiveTrue(id);

		if (result.isEmpty()) {
			return Map.of("success", false, "message", "Comment not found");
		}

		Comment comment = result.get();

		boolean isAuthor = callerId != null && callerId.equals(comment.getUserId());
		if (!isAuthor && !isTargetOwner(callerId, comment)) {
			return Map.of("success", false, "message", "Author or content owner only");
		}

		commentRepository.deactivateSubtree(id);
		return Map.of("success", true, "message", "Comment deleted");
	}

	private boolean canViewTarget(Long viewerId, Comment comment) {
		if (comment.getVideoId() != null) {
			return videoRepository.findByIdAndIsActiveTrue(comment.getVideoId())
					.map(v -> canViewVideo(viewerId, v))
					.orElse(false);
		}
		if (comment.getPostId() != null) {
			return postRepository.findByIdAndIsActiveTrue(comment.getPostId())
					.map(p -> canViewPost(viewerId, p))
					.orElse(false);
		}
		return false;
	}

	private boolean canViewVideo(Long viewerId, Video video) {
		if (viewerId != null && viewerId.equals(video.getUserId())) {
			return true;
		}
		if (video.getVisibility() == Video.Visibility.PRIVATE) {
			return false;
		}
		return canViewAccountContent(viewerId, video.getUserId());
	}

	private boolean canViewPost(Long viewerId, Post post) {
		if (viewerId != null && viewerId.equals(post.getUserId())) {
			return true;
		}
		if (post.getVisibility() == Post.Visibility.PRIVATE) {
			return false;
		}
		return canViewAccountContent(viewerId, post.getUserId());
	}

	private boolean canViewAccountContent(Long viewerId, Long ownerId) {
		Optional<User> owner = userRepository.findByIdAndIsActiveTrue(ownerId);
		if (owner.isEmpty()) {
			return false;
		}
		if (owner.get().getVisibility() != User.Visibility.PRIVATE) {
			return true;
		}
		return viewerId != null && isApprovedSubscriber(viewerId, ownerId);
	}

	private boolean isApprovedSubscriber(Long viewerId, Long ownerId) {
		return subscriptionRepository.findBySubscriberIdAndChannelIdAndIsActiveTrue(viewerId, ownerId)
				.map(s -> s.getStatus() == Subscription.Status.APPROVED)
				.orElse(false);
	}

	private boolean isTargetOwner(Long callerId, Comment comment) {
		if (callerId == null) {
			return false;
		}
		if (comment.getVideoId() != null) {
			return videoRepository.findByIdAndIsActiveTrue(comment.getVideoId())
					.map(v -> callerId.equals(v.getUserId()))
					.orElse(false);
		}
		if (comment.getPostId() != null) {
			return postRepository.findByIdAndIsActiveTrue(comment.getPostId())
					.map(p -> callerId.equals(p.getUserId()))
					.orElse(false);
		}
		return false;
	}

	private boolean commentsDisabledForTarget(Comment comment) {
		if (comment.getVideoId() == null) {
			return false;
		}
		return videoRepository.findByIdAndIsActiveTrue(comment.getVideoId())
				.map(v -> Boolean.TRUE.equals(v.getCommentsDisabled()))
				.orElse(false);
	}

	private Slice<CommentResponse> toResponseSlice(Slice<Comment> slice) {
		Map<Long, User> authors = loadAuthors(slice.getContent());
		Map<Long, Long> replyCounts = loadReplyCounts(slice.getContent());

		List<CommentResponse> content = new ArrayList<>();
		for (Comment comment : slice.getContent()) {
			User author = authors.get(comment.getUserId());
			if (author == null) {
				continue;
			}
			content.add(CommentResponse.from(comment, author, replyCounts.get(comment.getId())));
		}
		return new SliceImpl<>(content, slice.getPageable(), slice.hasNext());
	}

	private Map<Long, User> loadAuthors(Collection<Comment> comments) {
		Set<Long> ids = new LinkedHashSet<>();
		for (Comment comment : comments) {
			if (comment.getUserId() != null) {
				ids.add(comment.getUserId());
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

	private Map<Long, Long> loadReplyCounts(Collection<Comment> comments) {
		Set<Long> ids = new LinkedHashSet<>();
		for (Comment comment : comments) {
			if (comment.getId() != null) {
				ids.add(comment.getId());
			}
		}
		if (ids.isEmpty()) {
			return Map.of();
		}
		Map<Long, Long> counts = new HashMap<>();
		for (Object[] row : commentRepository.countRepliesByParentIdIn(ids)) {
			Object parentId = row[0];
			Object count = row[1];
			if (parentId != null && count != null) {
				counts.put(((Number) parentId).longValue(), ((Number) count).longValue());
			}
		}
		return counts;
	}

	private String normalizeContent(String content) {
		if (content == null) {
			return null;
		}
		String trimmed = content.trim();
		return trimmed.isEmpty() ? null : trimmed;
	}
}
