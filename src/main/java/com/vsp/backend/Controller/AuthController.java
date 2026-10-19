package com.vsp.backend.Controller;

import com.vsp.backend.Dto.LoginRequest;
import com.vsp.backend.Dto.UserResponse;
import com.vsp.backend.Model.User;
import com.vsp.backend.Repository.UserRepository;
import com.vsp.backend.Service.JwtService;
import io.jsonwebtoken.Claims;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpHeaders;
import org.springframework.http.ResponseCookie;
import org.springframework.http.ResponseEntity;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.web.bind.annotation.CookieValue;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.time.Duration;
import java.util.Map;
import java.util.Optional;

@RestController
@RequestMapping("/api/v1/auth")
public class AuthController {

	private static final String REFRESH_COOKIE_NAME = "refreshToken";
	private static final String REFRESH_COOKIE_PATH = "/api/v1/auth";

	private final UserRepository userRepository;
	private final BCryptPasswordEncoder passwordEncoder;
	private final JwtService jwtService;
	private final boolean refreshCookieSecure;

	public AuthController(UserRepository userRepository, BCryptPasswordEncoder passwordEncoder, JwtService jwtService,
			@Value("${jwt.refresh-cookie-secure:false}") boolean refreshCookieSecure) {
		this.userRepository = userRepository;
		this.passwordEncoder = passwordEncoder;
		this.jwtService = jwtService;
		this.refreshCookieSecure = refreshCookieSecure;
	}

	@PostMapping("/login")
	public ResponseEntity<Map<String, Object>> login(@RequestBody LoginRequest request) {
		if (request == null || request.email() == null || request.email().isBlank()
				|| request.password() == null || request.password().isBlank()) {
			return ResponseEntity.badRequest()
					.body(Map.of("success", false, "message", "Email and password are required"));
		}

		Optional<User> result = userRepository
				.findByEmailIgnoreCaseAndIsActiveTrue(request.email().trim().toLowerCase());

		if (result.isEmpty() || !passwordEncoder.matches(request.password(), result.get().getPasswordHash())) {
			return ResponseEntity.status(401).body(Map.of("success", false, "message", "Invalid email or password"));
		}

		User user = result.get();
		String accessToken = jwtService.generateToken(user.getId(), user.getUsername(), user.getEmail());
		String refreshToken = jwtService.generateRefreshToken(user.getId(), user.getUsername(), user.getEmail());

		return ResponseEntity.ok()
				.header(HttpHeaders.SET_COOKIE, refreshCookie(refreshToken).toString())
				.body(Map.of(
						"success", true,
						"message", "Login successful",
						"accessToken", accessToken,
						"user", UserResponse.fullView(user)));
	}

	@PostMapping("/refresh")
	public ResponseEntity<Map<String, Object>> refresh(
			@CookieValue(name = REFRESH_COOKIE_NAME, required = false) String refreshToken) {
		if (refreshToken == null || refreshToken.isBlank()) {
			return ResponseEntity.status(401).body(Map.of("success", false, "message", "Missing refresh token"));
		}

		Optional<Claims> parsed = jwtService.parse(refreshToken);
		if (parsed.isEmpty() || !jwtService.isRefreshToken(parsed.get())) {
			return invalidRefreshToken();
		}

		Long userId = jwtService.extractUserId(parsed.get());
		if (userId == null) {
			return invalidRefreshToken();
		}

		Optional<User> result = userRepository.findByIdAndIsActiveTrue(userId);
		if (result.isEmpty()) {
			return invalidRefreshToken();
		}

		User user = result.get();
		String accessToken = jwtService.generateToken(user.getId(), user.getUsername(), user.getEmail());
		String rotatedRefreshToken = jwtService.generateRefreshToken(user.getId(), user.getUsername(), user.getEmail());

		return ResponseEntity.ok()
				.header(HttpHeaders.SET_COOKIE, refreshCookie(rotatedRefreshToken).toString())
				.body(Map.of(
						"success", true,
						"message", "Token refreshed",
						"accessToken", accessToken));
	}

	@PostMapping("/logout")
	public ResponseEntity<Map<String, Object>> logout() {
		ResponseCookie cleared = ResponseCookie.from(REFRESH_COOKIE_NAME, "")
				.httpOnly(true)
				.secure(refreshCookieSecure)
				.sameSite("Strict")
				.path(REFRESH_COOKIE_PATH)
				.maxAge(Duration.ZERO)
				.build();

		return ResponseEntity.ok()
				.header(HttpHeaders.SET_COOKIE, cleared.toString())
				.body(Map.of("success", true, "message", "Logged out"));
	}

	private ResponseEntity<Map<String, Object>> invalidRefreshToken() {
		return ResponseEntity.status(401).body(Map.of("success", false, "message", "Invalid refresh token"));
	}

	private ResponseCookie refreshCookie(String refreshToken) {
		return ResponseCookie.from(REFRESH_COOKIE_NAME, refreshToken)
				.httpOnly(true)
				.secure(refreshCookieSecure)
				.sameSite("Strict")
				.path(REFRESH_COOKIE_PATH)
				.maxAge(Duration.ofMillis(jwtService.getRefreshExpirationMs()))
				.build();
	}
}
