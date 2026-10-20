package com.vsp.backend.Controller;

import java.util.Map;

import org.springframework.data.domain.Slice;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.vsp.backend.Dto.PostRequest;
import com.vsp.backend.Dto.PostResponse;
import com.vsp.backend.Security.AppUserPrincipal;
import com.vsp.backend.Service.PostService;

@RestController
@RequestMapping("/api/v1/posts")
public class PostController {

    private final PostService postService;

    public PostController(PostService postService) {
        this.postService = postService;
    }

    @GetMapping
    public ResponseEntity<?> listPosts(@RequestParam(required = false) Long userId,
            @RequestParam(required = false) String username,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size,
            @AuthenticationPrincipal AppUserPrincipal me) {
        int pageSize = Math.min(Math.max(size, 1), 100);
        Long viewerId = me != null ? me.id() : null;

        if (username != null && !username.isBlank()) {
            Slice<PostResponse> posts = postService.getChannelPostsByUsername(username, viewerId, page, pageSize);

            if (posts == null) {
                return ResponseEntity.status(404).body(Map.of("success", false, "message", "User not found or deactivated"));
            }

            return ResponseEntity.ok(Map.of("success", true, "posts", posts));
        }

        if (userId != null) {
            Slice<PostResponse> posts = postService.getChannelPosts(userId, viewerId, page, pageSize);

            if (posts == null) {
                return ResponseEntity.status(404).body(Map.of("success", false, "message", "User not found or deactivated"));
            }

            return ResponseEntity.ok(Map.of("success", true, "posts", posts));
        }

        return ResponseEntity.ok(Map.of("success", true, "posts", postService.getHomeFeed(page, pageSize)));
    }

    @GetMapping("/{id}")
    public ResponseEntity<?> getPost(@PathVariable Long id,
            @AuthenticationPrincipal AppUserPrincipal me) {
        Long viewerId = me != null ? me.id() : null;
        PostResponse post = postService.getById(id, viewerId);

        if (post != null) {
            return ResponseEntity.ok(Map.of("success", true, "post", post));
        }

        return ResponseEntity.status(404).body(Map.of("success", false, "message", "Post not found or private"));
    }

    @PostMapping
    public ResponseEntity<Map<String, Object>> createPost(@AuthenticationPrincipal AppUserPrincipal me,
            @RequestBody PostRequest request) {
        Map<String, Object> result = postService.create(request, me.id());

        return result.get("success").equals(true)
                ? ResponseEntity.ok(result)
                : ResponseEntity.badRequest().body(result);
    }

    @PutMapping("/{id}")
    public ResponseEntity<Map<String, Object>> updatePost(@PathVariable Long id,
            @AuthenticationPrincipal AppUserPrincipal me,
            @RequestBody PostRequest changes) {
        Map<String, Object> result = postService.updateById(id, changes, me.id());

        return result.get("success").equals(true)
                ? ResponseEntity.ok(result)
                : ResponseEntity.badRequest().body(result);
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Map<String, Object>> deletePost(@PathVariable Long id,
            @AuthenticationPrincipal AppUserPrincipal me) {
        Map<String, Object> result = postService.deleteById(id, me.id());

        return result.get("success").equals(true)
                ? ResponseEntity.ok(result)
                : ResponseEntity.badRequest().body(result);
    }
}
