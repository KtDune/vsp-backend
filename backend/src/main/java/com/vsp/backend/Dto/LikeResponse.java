package com.vsp.backend.Dto;

import com.vsp.backend.Model.Like;
import java.time.LocalDateTime;

public record LikeResponse(Long id, Long userId, String targetType, Long targetId, LocalDateTime createdAt) {

    public static LikeResponse from(Like like) {
        return new LikeResponse(like.getId(), like.getUserId(),
                like.getTargetType() != null ? like.getTargetType().name() : null,
                like.getTargetId(), like.getCreatedAt());
    }
}
