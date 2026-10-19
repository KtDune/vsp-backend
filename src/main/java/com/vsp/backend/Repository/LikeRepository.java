package com.vsp.backend.Repository;

import com.vsp.backend.Model.Like;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface LikeRepository extends JpaRepository<Like, Long> {

    Optional<Like> findByUserIdAndTargetTypeAndTargetId(Long userId, Like.TargetType targetType, Long targetId);

    long countByTargetTypeAndTargetIdAndIsActiveTrue(Like.TargetType targetType, Long targetId);

    boolean existsByUserIdAndTargetTypeAndTargetIdAndIsActiveTrue(Long userId, Like.TargetType targetType,
            Long targetId);
}
