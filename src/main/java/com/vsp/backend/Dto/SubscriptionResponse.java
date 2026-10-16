package com.vsp.backend.Dto;

import com.vsp.backend.Model.Subscription;
import java.time.LocalDateTime;

public record SubscriptionResponse(Long id, Long subscriberId, Long channelId, String status,
        LocalDateTime createdAt, LocalDateTime decidedAt) {

    public static SubscriptionResponse fullView(Subscription subscription) {
        return new SubscriptionResponse(subscription.getId(), subscription.getSubscriberId(),
                subscription.getChannelId(),
                subscription.getStatus() != null ? subscription.getStatus().name() : null,
                subscription.getCreatedAt(), subscription.getDecidedAt());
    }
}
