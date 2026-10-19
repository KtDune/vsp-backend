package com.vsp.backend.Dto;

public record CommentRequest(String content, Long videoId, Long postId, Long parentId) {
}
