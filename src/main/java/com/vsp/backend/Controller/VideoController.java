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
import com.vsp.backend.Service.VideoService;

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
            @RequestParam(required = false) Long viewerId,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {
        int pageSize = Math.min(Math.max(size, 1), 100);

        if (userId != null) {
            Slice<Video> videos = videoService.getChannelVideos(userId, viewerId, page, pageSize);

            if (videos == null) {
                return ResponseEntity.notFound().build();
            }

            return ResponseEntity.ok(videos);
        }

        return ResponseEntity.ok(videoService.getHomeFeed(page, pageSize));
    }

    @GetMapping("/{id}")
    public ResponseEntity<VideoResponse> watchVideo(@PathVariable Long id,
            @RequestParam(required = false) Long viewerId) {
        VideoResponse v = videoService.getById(id, viewerId);

        if (v != null) {
            return ResponseEntity.ok(v);
        }

        return ResponseEntity.notFound().build();
    }

    @PostMapping
    public ResponseEntity<Map<String, Object>> createVideo(@RequestBody Video video) {
        Map<String, Object> result = videoService.create(video);

        return result.get("success").equals(true)
                ? ResponseEntity.ok(result)
                : ResponseEntity.badRequest().body(result);
    }

    @PutMapping("/{id}")
    public ResponseEntity<Map<String, Object>> updateVideo(@PathVariable Long id, @RequestBody Video changes) {
        Map<String, Object> result = videoService.updateById(id, changes);

        return result.get("success").equals(true)
                ? ResponseEntity.ok(result)
                : ResponseEntity.badRequest().body(result);
    }
}
