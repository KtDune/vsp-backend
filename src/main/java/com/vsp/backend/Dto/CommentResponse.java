package com.vsp.backend.Dto;

import com.vsp.backend.Model.Comment;
import com.vsp.backend.Model.User;
import java.time.LocalDateTime;

public record CommentResponse(Long id, Long userId, Long videoId, Long postId, Long parentId, String content,
        Boolean isEdited, LocalDateTime createdAt, Long replyCount, String authorUsername, String authorProfileUrl) {

    public static CommentResponse from(Comment comment, User author) {
        return from(comment, author, null);
    }

    public static CommentResponse from(Comment comment, User author, Long replyCount) {
        return new CommentResponse(comment.getId(), comment.getUserId(), comment.getVideoId(), comment.getPostId(),
                comment.getParentId(), comment.getContent(), comment.getIsEdited(), comment.getCreatedAt(),
                replyCount, author.getUsername(), author.getProfileUrl());
    }
}
