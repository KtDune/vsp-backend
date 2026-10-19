package com.vsp.backend.Controller;

import org.springframework.data.domain.Slice;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.vsp.backend.Dto.VideoResponse;
import com.vsp.backend.Model.Video;
import com.vsp.backend.Security.AppUserPrincipal;
import com.vsp.backend.Service.VideoService;
import org.springframework.security.core.annotation.AuthenticationPrincipal;

import java.util.Map;

@RestController
@RequestMapping("/api/v1/videos")
public class VideoController {

    private final VideoService videoService;

    public VideoController(VideoService videoService) {
        this.videoService = videoService;
    }

    @GetMapping
    public ResponseEntity<?> listVideos(@RequestParam(required = false) Long userId,
            @RequestParam(required = false) String username,
            @RequestParam(required = false) String title,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size,
            @AuthenticationPrincipal AppUserPrincipal me) {
        int pageSize = Math.min(Math.max(size, 1), 100);
        Long viewerId = me != null ? me.id() : null;

        if (title != null && !title.isBlank()) {
            return ResponseEntity.ok(Map.of("success", true, "videos", videoService.searchByTitle(title, page, pageSize)));
        }

        if (username != null && !username.isBlank()) {
            Slice<VideoResponse> videos = videoService.getChannelVideosByUsername(username, viewerId, page, pageSize);

            if (videos == null) {
                return ResponseEntity.status(404).body(Map.of("success", false, "message", "User not found or deactivated"));
            }

            return ResponseEntity.ok(Map.of("success", true, "videos", videos));
        }

        if (userId != null) {
            Slice<VideoResponse> videos = videoService.getChannelVideos(userId, viewerId, page, pageSize);

            if (videos == null) {
                return ResponseEntity.status(404).body(Map.of("success", false, "message", "User not found or deactivated"));
            }

            return ResponseEntity.ok(Map.of("success", true, "videos", videos));
        }

        return ResponseEntity.ok(Map.of("success", true, "videos", videoService.getHomeFeed(page, pageSize)));
    }

    @GetMapping("/{id}")
    public ResponseEntity<?> watchVideo(@PathVariable Long id,
            @AuthenticationPrincipal AppUserPrincipal me) {
        Long viewerId = me != null ? me.id() : null;
        VideoResponse v = videoService.getById(id, viewerId);

        if (v != null) {
            return ResponseEntity.ok(Map.of("success", true, "video", v));
        }

        return ResponseEntity.status(404).body(Map.of("success", false, "message", "Video not found or private"));
    }

    @PostMapping
    public ResponseEntity<Map<String, Object>> createVideo(@AuthenticationPrincipal AppUserPrincipal me,
            @RequestBody Video video) {
        Map<String, Object> result = videoService.create(video, me.id());

        return result.get("success").equals(true)
                ? ResponseEntity.ok(result)
                : ResponseEntity.badRequest().body(result);
    }

    @PutMapping("/{id}")
    public ResponseEntity<Map<String, Object>> updateVideo(@PathVariable Long id,
            @AuthenticationPrincipal AppUserPrincipal me,
            @RequestBody Video changes) {
        Map<String, Object> result = videoService.updateById(id, changes, me.id());

        return result.get("success").equals(true)
                ? ResponseEntity.ok(result)
                : ResponseEntity.badRequest().body(result);
    }
}
