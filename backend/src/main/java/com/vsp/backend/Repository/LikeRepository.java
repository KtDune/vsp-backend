package com.vsp.backend.Repository;

import com.vsp.backend.Model.Like;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

@Repository
public interface LikeRepository extends JpaRepository<Like, Long> {

    Optional<Like> findByUserIdAndTargetTypeAndTargetId(Long userId, Like.TargetType targetType, Long targetId);

    long countByTargetTypeAndTargetIdAndIsActiveTrue(Like.TargetType targetType, Long targetId);

    boolean existsByUserIdAndTargetTypeAndTargetIdAndIsActiveTrue(Long userId, Like.TargetType targetType,
            Long targetId);

    @Modifying
    @Query("UPDATE Like l SET l.isActive = false WHERE l.targetType = :targetType AND l.targetId = :targetId AND l.isActive = true")
    int deactivateByTarget(@Param("targetType") Like.TargetType targetType, @Param("targetId") Long targetId);
}
