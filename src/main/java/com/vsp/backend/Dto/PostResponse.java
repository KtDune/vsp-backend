package com.vsp.backend.Dto;

import com.vsp.backend.Model.Post;
import com.vsp.backend.Model.User;
import java.time.LocalDateTime;
import java.util.List;

public record PostResponse(Long id, Long userId, String content, String visibility, Boolean isEdited,
        LocalDateTime createdAt, LocalDateTime updatedAt, List<String> imageUrls,
        String authorUsername, String authorProfileUrl) {

    public static PostResponse from(Post post, List<String> imageUrls, User author) {
        return new PostResponse(post.getId(), post.getUserId(), post.getContent(),
                post.getVisibility() != null ? post.getVisibility().name() : null,
                post.getIsEdited(), post.getCreatedAt(), post.getUpdatedAt(), imageUrls,
                author.getUsername(), author.getProfileUrl());
    }
}
