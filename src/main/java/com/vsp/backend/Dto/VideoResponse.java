package com.vsp.backend.Dto;

import com.vsp.backend.Model.User;
import com.vsp.backend.Model.Video;
import java.time.LocalDateTime;

public record VideoResponse(Long id, Long userId, String title, String description, String visibility,
        String thumbnailUrl, String videoUrl, Integer duration, Integer viewCount, LocalDateTime createdAt,
        String channelName, String channelAvatarUrl, Boolean commentsDisabled) {

    public static VideoResponse fullView(Video video) {
        return new VideoResponse(video.getId(), video.getUserId(), video.getTitle(), video.getDescription(),
                video.getVisibility() != null ? video.getVisibility().name() : null,
                video.getThumbnailUrl(), video.getVideoUrl(), video.getDuration(), video.getViewCount(),
                video.getCreatedAt(), null, null, video.getCommentsDisabled());
    }

    public static VideoResponse fullView(Video video, User channel) {
        return new VideoResponse(video.getId(), video.getUserId(), video.getTitle(), video.getDescription(),
                video.getVisibility() != null ? video.getVisibility().name() : null,
                video.getThumbnailUrl(), video.getVideoUrl(), video.getDuration(), video.getViewCount(),
                video.getCreatedAt(), channel.getUsername(), channel.getProfileUrl(), video.getCommentsDisabled());
    }

    public static VideoResponse cardView(Video video, User channel) {
        return new VideoResponse(video.getId(), video.getUserId(), video.getTitle(), null,
                video.getVisibility() != null ? video.getVisibility().name() : null,
                video.getThumbnailUrl(), null, video.getDuration(), video.getViewCount(),
                video.getCreatedAt(), channel.getUsername(), channel.getProfileUrl(), null);
    }
}
