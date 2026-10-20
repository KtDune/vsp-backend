package com.vsp.backend.Service;

import com.vsp.backend.Dto.LikeResponse;
import com.vsp.backend.Model.Comment;
import com.vsp.backend.Model.Like;
import com.vsp.backend.Model.Post;
import com.vsp.backend.Model.Subscription;
import com.vsp.backend.Model.User;
import com.vsp.backend.Model.Video;
import com.vsp.backend.Repository.CommentRepository;
import com.vsp.backend.Repository.LikeRepository;
import com.vsp.backend.Repository.PostRepository;
import com.vsp.backend.Repository.SubscriptionRepository;
import com.vsp.backend.Repository.UserRepository;
import com.vsp.backend.Repository.VideoRepository;

import java.util.Map;
import java.util.Optional;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class LikeService {

    private final LikeRepository likeRepository;
    private final UserRepository userRepository;
    private final VideoRepository videoRepository;
    private final PostRepository postRepository;
    private final CommentRepository commentRepository;
    private final SubscriptionRepository subscriptionRepository;

    public LikeService(LikeRepository likeRepository, UserRepository userRepository,
            VideoRepository videoRepository, PostRepository postRepository,
            CommentRepository commentRepository, SubscriptionRepository subscriptionRepository) {
        this.likeRepository = likeRepository;
        this.userRepository = userRepository;
        this.videoRepository = videoRepository;
        this.postRepository = postRepository;
        this.commentRepository = commentRepository;
        this.subscriptionRepository = subscriptionRepository;
    }

    @Transactional
    public Map<String, Object> like(Like.TargetType targetType, Long targetId, Long callerId) {
        if (callerId == null) {
            return Map.of("success", false, "message", "Authentication required");
        }
        if (targetId == null) {
            return Map.of("success", false, "message", "Target id is required");
        }
        if (userRepository.findByIdAndIsActiveTrue(callerId).isEmpty()) {
            return Map.of("success", false, "message", "User not found or deactivated");
        }
        if (!targetIsVisible(callerId, targetType, targetId)) {
            return Map.of("success", false, "message", targetNotFoundMessage(targetType));
        }

        Optional<Like> existingResult = likeRepository.findByUserIdAndTargetTypeAndTargetId(callerId, targetType,
                targetId);
        if (existingResult.isPresent()) {
            Like existing = existingResult.get();
            if (Boolean.TRUE.equals(existing.getIsActive())) {
                return Map.of("success", true, "message", "Already liked", "like", LikeResponse.from(existing));
            }
            existing.setIsActive(true);
            likeRepository.save(existing);
            return Map.of("success", true, "message", "Liked", "like", LikeResponse.from(existing));
        }

        Like like = new Like();
        like.setUserId(callerId);
        like.setTargetType(targetType);
        like.setTargetId(targetId);
        like.setIsActive(true);

        Like saved = likeRepository.save(like);
        return Map.of("success", true, "message", "Liked", "like", LikeResponse.from(saved));
    }

    @Transactional
    public Map<String, Object> unlike(Like.TargetType targetType, Long targetId, Long callerId) {
        if (callerId == null) {
            return Map.of("success", false, "message", "Authentication required");
        }
        if (targetId == null) {
            return Map.of("success", false, "message", "Target id is required");
        }

        Optional<Like> existingResult = likeRepository.findByUserIdAndTargetTypeAndTargetId(callerId, targetType,
                targetId);
        if (existingResult.isEmpty() || !Boolean.TRUE.equals(existingResult.get().getIsActive())) {
            return Map.of("success", true, "message", "Already unliked");
        }

        Like existing = existingResult.get();
        existing.setIsActive(false);
        likeRepository.save(existing);
        return Map.of("success", true, "message", "Like removed", "like", LikeResponse.from(existing));
    }

    public Map<String, Object> getStatus(Like.TargetType targetType, Long targetId, Long viewerId) {
        if (targetId == null) {
            return Map.of("success", false, "message", "Target id is required");
        }
        if (!targetIsVisible(viewerId, targetType, targetId)) {
            return Map.of("success", false, "message", targetNotFoundMessage(targetType));
        }

        boolean likedByMe = false;
        if (viewerId != null) {
            likedByMe = likeRepository.existsByUserIdAndTargetTypeAndTargetIdAndIsActiveTrue(viewerId, targetType,
                    targetId);
        }

        long likeCount = likeRepository.countByTargetTypeAndTargetIdAndIsActiveTrue(targetType, targetId);
        return Map.of("success", true, "likedByMe", likedByMe, "likeCount", likeCount);
    }

    private boolean targetIsVisible(Long viewerId, Like.TargetType targetType, Long targetId) {
        if (targetType == null) {
            return false;
        }
        if (targetType == Like.TargetType.VIDEO) {
            Optional<Video> video = videoRepository.findByIdAndIsActiveTrue(targetId);
            return video.isPresent() && canViewVideo(viewerId, video.get());
        }
        if (targetType == Like.TargetType.POST) {
            Optional<Post> post = postRepository.findByIdAndIsActiveTrue(targetId);
            return post.isPresent() && canViewPost(viewerId, post.get());
        }
        if (targetType == Like.TargetType.COMMENT) {
            Optional<Comment> comment = commentRepository.findByIdAndIsActiveTrue(targetId);
            return comment.isPresent() && canViewCommentTarget(viewerId, comment.get());
        }
        return false;
    }

    private boolean canViewCommentTarget(Long viewerId, Comment comment) {
        if (comment.getVideoId() != null) {
            Optional<Video> video = videoRepository.findByIdAndIsActiveTrue(comment.getVideoId());
            return video.isPresent() && canViewVideo(viewerId, video.get());
        }
        if (comment.getPostId() != null) {
            Optional<Post> post = postRepository.findByIdAndIsActiveTrue(comment.getPostId());
            return post.isPresent() && canViewPost(viewerId, post.get());
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

    private String targetNotFoundMessage(Like.TargetType targetType) {
        if (targetType == Like.TargetType.VIDEO) {
            return "Video not found or private";
        }
        if (targetType == Like.TargetType.POST) {
            return "Post not found or private";
        }
        return "Comment not found or private";
    }
}
