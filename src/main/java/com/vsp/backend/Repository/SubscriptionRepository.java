package com.vsp.backend.Repository;

import com.vsp.backend.Model.Subscription;
import java.time.LocalDateTime;
import java.util.Optional;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Slice;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

@Repository
public interface SubscriptionRepository extends JpaRepository<Subscription, Long> {

    Optional<Subscription> findByIdAndIsActiveTrue(Long id);

    Optional<Subscription> findBySubscriberIdAndChannelIdAndIsActiveTrue(Long subscriberId, Long channelId);

    Slice<Subscription> findBySubscriberIdAndStatusAndIsActiveTrue(Long subscriberId, Subscription.Status status, Pageable pageable);

    Slice<Subscription> findByChannelIdAndStatusAndIsActiveTrue(Long channelId, Subscription.Status status, Pageable pageable);

    long countByChannelIdAndStatusAndIsActiveTrue(Long channelId, Subscription.Status status);

    @Modifying
    @Query("UPDATE Subscription s SET s.status = 'APPROVED', s.decidedAt = :now WHERE s.channelId = :userId AND s.status = 'PENDING' AND s.isActive = true")
    int approveAllPendingForChannel(@Param("userId") Long userId, @Param("now") LocalDateTime now);
}
