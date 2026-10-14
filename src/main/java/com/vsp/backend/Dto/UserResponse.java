package com.vsp.backend.Dto;

import com.vsp.backend.Model.User;
import java.time.LocalDateTime;

public record UserResponse(Long id, String username, String visibility, LocalDateTime createdAt) {

    public static UserResponse publicView(User user) {
        return new UserResponse(user.getId(), user.getUsername(), null, null);
    }

    public static UserResponse fullView(User user) {
        return new UserResponse(user.getId(), user.getUsername(), user.getVisibility().name(), user.getCreatedAt());
    }
}
