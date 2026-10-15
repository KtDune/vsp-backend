package com.vsp.backend.Repository;

import com.vsp.backend.Model.User;
import com.vsp.backend.Model.Video;
import java.util.Optional;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Slice;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

@Repository
public interface VideoRepository extends JpaRepository<Video, Long> {

    Optional<Video> findByIdAndIsActiveTrue(Long id);

    Slice<Video> findByUserIdAndIsActiveTrue(Long userId, Pageable pageable);

    Slice<Video> findByUserIdAndIsActiveTrueAndVisibility(Long userId, Video.Visibility visibility, Pageable pageable);

    @Query("SELECT v FROM Video v WHERE v.isActive = true AND v.visibility = :visibility AND v.userId IN (SELECT u.id FROM User u WHERE u.isActive = true AND u.visibility = :accountVisibility)")
    Slice<Video> findPublicFeed(@Param("visibility") Video.Visibility visibility, @Param("accountVisibility") User.Visibility accountVisibility, Pageable pageable);

    @Modifying
    @Query("UPDATE Video v SET v.viewCount = v.viewCount + 1 WHERE v.id = :id")
    int incrementViewCount(@Param("id") Long id);
}
