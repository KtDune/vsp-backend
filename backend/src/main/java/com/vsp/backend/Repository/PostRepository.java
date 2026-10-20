package com.vsp.backend.Repository;

import com.vsp.backend.Model.Post;
import com.vsp.backend.Model.User;
import java.util.Optional;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Slice;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

@Repository
public interface PostRepository extends JpaRepository<Post, Long> {

    Optional<Post> findByIdAndIsActiveTrue(Long id);

    Slice<Post> findByUserIdAndIsActiveTrue(Long userId, Pageable pageable);

    Slice<Post> findByUserIdAndIsActiveTrueAndVisibility(Long userId, Post.Visibility visibility, Pageable pageable);

    @Query("SELECT p FROM Post p WHERE p.isActive = true AND p.visibility = :visibility AND p.userId IN (SELECT u.id FROM User u WHERE u.isActive = true AND u.visibility = :accountVisibility)")
    Slice<Post> findPublicFeed(@Param("visibility") Post.Visibility visibility, @Param("accountVisibility") User.Visibility accountVisibility, Pageable pageable);
}
