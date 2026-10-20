package com.vsp.backend.Security;

import com.vsp.backend.Model.User;
import com.vsp.backend.Repository.UserRepository;
import com.vsp.backend.Service.JwtService;
import io.jsonwebtoken.Claims;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.web.authentication.WebAuthenticationDetailsSource;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.List;
import java.util.Optional;

@Component
public class JwtAuthFilter extends OncePerRequestFilter {

	private final JwtService jwtService;
	private final UserRepository userRepository;

	public JwtAuthFilter(JwtService jwtService, UserRepository userRepository) {
		this.jwtService = jwtService;
		this.userRepository = userRepository;
	}

	@Override
	protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain chain)
			throws ServletException, IOException {

		String header = request.getHeader("Authorization");

		if (header != null && header.startsWith("Bearer ")
				&& SecurityContextHolder.getContext().getAuthentication() == null) {
			String token = header.substring(7);
			Optional<Claims> claims = jwtService.parse(token);

			if (claims.isPresent() && !jwtService.isRefreshToken(claims.get())) {
				Long userId = jwtService.extractUserId(claims.get());

				if (userId != null) {
					Optional<User> user = userRepository.findByIdAndIsActiveTrue(userId);

					if (user.isPresent()) {
						AppUserPrincipal principal = new AppUserPrincipal(user.get().getId(),
								user.get().getUsername(), user.get().getEmail());
						UsernamePasswordAuthenticationToken authentication = UsernamePasswordAuthenticationToken
								.authenticated(principal, null, List.of());
						authentication.setDetails(new WebAuthenticationDetailsSource().buildDetails(request));
						SecurityContextHolder.getContext().setAuthentication(authentication);
					}
				}
			}
		}

		chain.doFilter(request, response);
	}
}
