package com.vsp.backend.Service;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.JwtException;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import javax.crypto.SecretKey;
import java.nio.charset.StandardCharsets;
import java.util.Date;
import java.util.Optional;

@Service
public class JwtService {

	public static final String TYPE_ACCESS = "access";
	public static final String TYPE_REFRESH = "refresh";

	private final SecretKey signingKey;
	private final long expirationMs;
	private final long refreshExpirationMs;

	public JwtService(@Value("${jwt.secret}") String secret,
			@Value("${jwt.expiration-ms:86400000}") long expirationMs,
			@Value("${jwt.refresh-expiration-ms:604800000}") long refreshExpirationMs) {
		this.signingKey = Keys.hmacShaKeyFor(secret.getBytes(StandardCharsets.UTF_8));
		this.expirationMs = expirationMs;
		this.refreshExpirationMs = refreshExpirationMs;
	}

	public String generateToken(Long userId, String username, String email) {
		return buildToken(userId, username, email, TYPE_ACCESS, expirationMs);
	}

	public String generateRefreshToken(Long userId, String username, String email) {
		return buildToken(userId, username, email, TYPE_REFRESH, refreshExpirationMs);
	}

	private String buildToken(Long userId, String username, String email, String type, long ttlMs) {
		Date now = new Date();
		return Jwts.builder()
				.subject(String.valueOf(userId))
				.claim("username", username)
				.claim("email", email)
				.claim("type", type)
				.issuedAt(now)
				.expiration(new Date(now.getTime() + ttlMs))
				.signWith(signingKey)
				.compact();
	}

	public boolean isRefreshToken(Claims claims) {
		return claims != null && TYPE_REFRESH.equals(claims.get("type", String.class));
	}

	public long getRefreshExpirationMs() {
		return refreshExpirationMs;
	}

	public Optional<Claims> parse(String token) {
		if (token == null || token.isBlank()) {
			return Optional.empty();
		}
		try {
			return Optional.of(Jwts.parser()
					.verifyWith(signingKey)
					.build()
					.parseSignedClaims(token)
					.getPayload());
		} catch (JwtException | IllegalArgumentException e) {
			return Optional.empty();
		}
	}

	public Long extractUserId(Claims claims) {
		if (claims == null || claims.getSubject() == null) {
			return null;
		}
		try {
			return Long.valueOf(claims.getSubject());
		} catch (NumberFormatException e) {
			return null;
		}
	}
}
