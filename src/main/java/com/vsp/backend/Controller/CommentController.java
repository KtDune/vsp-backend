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

import com.vsp.backend.Dto.CommentRequest;
import com.vsp.backend.Dto.CommentResponse;
import com.vsp.backend.Security.AppUserPrincipal;
import com.vsp.backend.Service.CommentService;

@RestController
@RequestMapping("/api/v1/comments")
public class CommentController {

    private final CommentService commentService;

    public CommentController(CommentService commentService) {
        this.commentService = commentService;
    }

    @PostMapping
    public ResponseEntity<Map<String, Object>> createComment(@AuthenticationPrincipal AppUserPrincipal me,
            @RequestBody CommentRequest request) {
        Map<String, Object> result = commentService.create(request, me.id());

        return result.get("success").equals(true)
                ? ResponseEntity.ok(result)
                : ResponseEntity.badRequest().body(result);
    }

    @GetMapping
    public ResponseEntity<?> listComments(@RequestParam(required = false) Long videoId,
            @RequestParam(required = false) Long postId,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size,
            @AuthenticationPrincipal AppUserPrincipal me) {
        int pageSize = Math.min(Math.max(size, 1), 100);
        Long viewerId = me != null ? me.id() : null;

        if (videoId != null && postId != null) {
            return ResponseEntity.badRequest().body(Map.of("success", false, "message", "Provide only one of videoId or postId"));
        }

        if (videoId != null) {
            Slice<CommentResponse> comments = commentService.listForVideo(videoId, viewerId, page, pageSize);

            if (comments == null) {
                return ResponseEntity.status(404).body(Map.of("success", false, "message", "Video not found or private"));
            }

            return ResponseEntity.ok(Map.of("success", true, "comments", comments));
        }

        if (postId != null) {
            Slice<CommentResponse> comments = commentService.listForPost(postId, viewerId, page, pageSize);

            if (comments == null) {
                return ResponseEntity.status(404).body(Map.of("success", false, "message", "Post not found or private"));
            }

            return ResponseEntity.ok(Map.of("success", true, "comments", comments));
        }

        return ResponseEntity.badRequest().body(Map.of("success", false, "message", "videoId or postId is required"));
    }

    @GetMapping("/{id}/replies")
    public ResponseEntity<?> listReplies(@PathVariable Long id,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size,
            @AuthenticationPrincipal AppUserPrincipal me) {
        int pageSize = Math.min(Math.max(size, 1), 100);
        Long viewerId = me != null ? me.id() : null;
        Slice<CommentResponse> replies = commentService.listReplies(id, viewerId, page, pageSize);

        if (replies == null) {
            return ResponseEntity.status(404).body(Map.of("success", false, "message", "Comment not found or private"));
        }

        return ResponseEntity.ok(Map.of("success", true, "replies", replies));
    }

    @PutMapping("/{id}")
    public ResponseEntity<Map<String, Object>> updateComment(@PathVariable Long id,
            @AuthenticationPrincipal AppUserPrincipal me,
            @RequestBody CommentRequest changes) {
        Map<String, Object> result = commentService.updateById(id, changes, me.id());

        return result.get("success").equals(true)
                ? ResponseEntity.ok(result)
                : ResponseEntity.badRequest().body(result);
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Map<String, Object>> deleteComment(@PathVariable Long id,
            @AuthenticationPrincipal AppUserPrincipal me) {
        Map<String, Object> result = commentService.deleteById(id, me.id());

        return result.get("success").equals(true)
                ? ResponseEntity.ok(result)
                : ResponseEntity.badRequest().body(result);
    }
}
