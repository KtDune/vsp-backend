package com.vsp.backend.Dto;

import java.util.List;

public record PostRequest(String content, List<String> imageUrls, String visibility) {
}
