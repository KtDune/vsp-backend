package com.vsp.backend.Controller;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.vsp.backend.Dto.UserResponse;
import com.vsp.backend.Model.User;
import com.vsp.backend.Security.AppUserPrincipal;
import com.vsp.backend.Service.UserService;
import org.springframework.security.core.annotation.AuthenticationPrincipal;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/v1/users")
public class UserController {

    private final UserService userService;

    public UserController(UserService userService) {
        this.userService = userService;
    }

    @GetMapping
    public ResponseEntity<?> searchUsers(@RequestParam(required = false) String name) {
        if (name == null || name.isBlank()) {
            return ResponseEntity.badRequest()
                    .body(Map.of("success", false, "message", "Query parameter 'name' is required"));
        }

        List<UserResponse> results = userService.searchByName(name);

        if (results.isEmpty()) {
            return ResponseEntity.status(404).body(Map.of("success", false, "message", "User not found"));
        }

        return ResponseEntity.ok(Map.of("success", true, "users", results));
    }

    @GetMapping("/{id}")
    public ResponseEntity<?> getUser(@PathVariable Long id,
            @AuthenticationPrincipal AppUserPrincipal me) {
        UserResponse u = userService.getById(id, me != null ? me.id() : null);

        if (u != null) {
        	return ResponseEntity.ok(Map.of("success", true, "user", u));
        }

        return ResponseEntity.status(404).body(Map.of("success", false, "message", "User not found"));
    }

    @GetMapping("/by-username/{username}")
    public ResponseEntity<?> getUserByUsername(@PathVariable String username,
            @AuthenticationPrincipal AppUserPrincipal me) {
        UserResponse u = userService.getByUsername(username, me != null ? me.id() : null);

        if (u != null) {
            return ResponseEntity.ok(Map.of("success", true, "user", u));
        }

        return ResponseEntity.status(404).body(Map.of("success", false, "message", "User not found"));
    }

    @PostMapping
    public ResponseEntity<Map<String, Object>> createUser(@RequestBody User user) {
        Map<String, Object> result = userService.create(user);

        return result.get("success").equals(true)
                ? ResponseEntity.ok(result)
                : ResponseEntity.badRequest().body(result);
    }

    @PutMapping("/{id}")
    public ResponseEntity<Map<String, Object>> updateUser(@PathVariable Long id,
            @AuthenticationPrincipal AppUserPrincipal me,
            @RequestBody User changes) {
        if (me == null || !me.id().equals(id)) {
            return ResponseEntity.status(403).body(Map.of("success", false, "message", "Owner only"));
        }

        Map<String, Object> result = userService.updateById(id, changes);

        return result.get("success").equals(true)
                ? ResponseEntity.ok(result)
                : ResponseEntity.badRequest().body(result);
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Map<String, Object>> deleteUser(@PathVariable Long id,
            @AuthenticationPrincipal AppUserPrincipal me,
            @RequestBody(required = false) User request) {
        if (me == null || !me.id().equals(id)) {
            return ResponseEntity.status(403).body(Map.of("success", false, "message", "Owner only"));
        }

        if (userService.deactivateById(id, request)) {
            return ResponseEntity.ok(Map.of("success", true, "message", "User deactivated"));
        }

        return ResponseEntity.badRequest().body(Map.of("success", false, "message", "User not found or invalid password"));
    }
}
