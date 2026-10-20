package com.vsp.backend.Dto;

import com.vsp.backend.Model.Subscription;
import com.vsp.backend.Model.User;
import java.time.LocalDateTime;

public record SubscriptionResponse(Long id, Long subscriberId, Long channelId, String status,
        LocalDateTime createdAt, LocalDateTime decidedAt,
        String channelUsername, String channelProfileUrl) {

    public static SubscriptionResponse fullView(Subscription subscription) {
        return channelView(subscription, null);
    }

    public static SubscriptionResponse channelView(Subscription subscription, User channel) {
        return new SubscriptionResponse(subscription.getId(), subscription.getSubscriberId(),
                subscription.getChannelId(),
                subscription.getStatus() != null ? subscription.getStatus().name() : null,
                subscription.getCreatedAt(), subscription.getDecidedAt(),
                channel != null ? channel.getUsername() : null,
                channel != null ? channel.getProfileUrl() : null);
    }
}
