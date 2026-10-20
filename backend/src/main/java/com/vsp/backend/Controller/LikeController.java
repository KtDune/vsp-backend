package com.vsp.backend.Controller;

import com.vsp.backend.Model.Like;
import com.vsp.backend.Security.AppUserPrincipal;
import com.vsp.backend.Service.LikeService;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;

@RestController
@RequestMapping("/api/v1")
public class LikeController {

    private final LikeService likeService;

    public LikeController(LikeService likeService) {
        this.likeService = likeService;
    }

    @PostMapping("/videos/{id}/like")
    public ResponseEntity<Map<String, Object>> likeVideo(@PathVariable Long id,
            @AuthenticationPrincipal AppUserPrincipal me) {
        return respond(likeService.like(Like.TargetType.VIDEO, id, me.id()));
    }

    @DeleteMapping("/videos/{id}/like")
    public ResponseEntity<Map<String, Object>> unlikeVideo(@PathVariable Long id,
            @AuthenticationPrincipal AppUserPrincipal me) {
        return respond(likeService.unlike(Like.TargetType.VIDEO, id, me.id()));
    }

    @PostMapping("/posts/{id}/like")
    public ResponseEntity<Map<String, Object>> likePost(@PathVariable Long id,
            @AuthenticationPrincipal AppUserPrincipal me) {
        return respond(likeService.like(Like.TargetType.POST, id, me.id()));
    }

    @DeleteMapping("/posts/{id}/like")
    public ResponseEntity<Map<String, Object>> unlikePost(@PathVariable Long id,
            @AuthenticationPrincipal AppUserPrincipal me) {
        return respond(likeService.unlike(Like.TargetType.POST, id, me.id()));
    }

    @PostMapping("/comments/{id}/like")
    public ResponseEntity<Map<String, Object>> likeComment(@PathVariable Long id,
            @AuthenticationPrincipal AppUserPrincipal me) {
        return respond(likeService.like(Like.TargetType.COMMENT, id, me.id()));
    }

    @DeleteMapping("/comments/{id}/like")
    public ResponseEntity<Map<String, Object>> unlikeComment(@PathVariable Long id,
            @AuthenticationPrincipal AppUserPrincipal me) {
        return respond(likeService.unlike(Like.TargetType.COMMENT, id, me.id()));
    }

    @GetMapping("/videos/{id}/like-status")
    public ResponseEntity<?> getVideoLikeStatus(@PathVariable Long id,
            @AuthenticationPrincipal AppUserPrincipal me) {
        Long viewerId = me != null ? me.id() : null;
        return respondStatus(likeService.getStatus(Like.TargetType.VIDEO, id, viewerId));
    }

    @GetMapping("/posts/{id}/like-status")
    public ResponseEntity<?> getPostLikeStatus(@PathVariable Long id,
            @AuthenticationPrincipal AppUserPrincipal me) {
        Long viewerId = me != null ? me.id() : null;
        return respondStatus(likeService.getStatus(Like.TargetType.POST, id, viewerId));
    }

    @GetMapping("/comments/{id}/like-status")
    public ResponseEntity<?> getCommentLikeStatus(@PathVariable Long id,
            @AuthenticationPrincipal AppUserPrincipal me) {
        Long viewerId = me != null ? me.id() : null;
        return respondStatus(likeService.getStatus(Like.TargetType.COMMENT, id, viewerId));
    }

    private ResponseEntity<Map<String, Object>> respond(Map<String, Object> result) {
        return result.get("success").equals(true)
                ? ResponseEntity.ok(result)
                : ResponseEntity.badRequest().body(result);
    }

    private ResponseEntity<?> respondStatus(Map<String, Object> result) {
        return result.get("success").equals(true)
                ? ResponseEntity.ok(result)
                : ResponseEntity.status(404).body(result);
    }
}
