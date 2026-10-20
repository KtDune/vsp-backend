package com.vsp.backend.Repository;

import com.vsp.backend.Model.Comment;
import java.util.Collection;
import java.util.List;
import java.util.Optional;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Slice;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

@Repository
public interface CommentRepository extends JpaRepository<Comment, Long> {

    Optional<Comment> findByIdAndIsActiveTrue(Long id);

    Slice<Comment> findByVideoIdAndIsActiveTrueAndParentIdIsNull(Long videoId, Pageable pageable);

    Slice<Comment> findByPostIdAndIsActiveTrueAndParentIdIsNull(Long postId, Pageable pageable);

    Slice<Comment> findByParentIdAndIsActiveTrue(Long parentId, Pageable pageable);

    @Query("SELECT c.parentId, COUNT(c) FROM Comment c WHERE c.parentId IN :ids AND c.isActive = true GROUP BY c.parentId")
    List<Object[]> countRepliesByParentIdIn(@Param("ids") Collection<Long> ids);

    @Modifying
    @Query(value = "UPDATE comments c JOIN (WITH RECURSIVE tree AS ("
            + "SELECT id FROM comments WHERE id = :id "
            + "UNION ALL SELECT d.id FROM comments d JOIN tree t ON d.parent_id = t.id"
            + ") SELECT id FROM tree) sub ON c.id = sub.id SET c.is_active = FALSE", nativeQuery = true)
    int deactivateSubtree(@Param("id") Long id);

    @Modifying
    @Query("UPDATE Comment c SET c.isActive = false WHERE c.videoId = :videoId AND c.isActive = true")
    int deactivateByVideoId(@Param("videoId") Long videoId);

    @Modifying
    @Query("UPDATE Comment c SET c.isActive = false WHERE c.postId = :postId AND c.isActive = true")
    int deactivateByPostId(@Param("postId") Long postId);
}
