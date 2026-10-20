package com.vsp.backend.Repository;

import com.vsp.backend.Model.PostImage;
import java.util.Collection;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

@Repository
public interface PostImageRepository extends JpaRepository<PostImage, Long> {

    List<PostImage> findByPostIdInAndIsActiveTrueOrderBySortOrderAscIdAsc(Collection<Long> postIds);

    @Modifying
    @Query("UPDATE PostImage pi SET pi.isActive = false WHERE pi.postId = :postId")
    int deactivateByPostId(@Param("postId") Long postId);
}
