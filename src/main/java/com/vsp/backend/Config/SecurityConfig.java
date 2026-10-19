package com.vsp.backend.Config;

import com.vsp.backend.Security.JwtAuthFilter;
import com.vsp.backend.Security.JwtAuthenticationEntryPoint;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;

@Configuration
public class SecurityConfig {

	private final JwtAuthFilter jwtAuthFilter;
	private final JwtAuthenticationEntryPoint authenticationEntryPoint;

	public SecurityConfig(JwtAuthFilter jwtAuthFilter, JwtAuthenticationEntryPoint authenticationEntryPoint) {
		this.jwtAuthFilter = jwtAuthFilter;
		this.authenticationEntryPoint = authenticationEntryPoint;
	}

	@Bean
	public SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
		http
				.csrf(csrf -> csrf.disable())
				.headers(headers -> headers
						.frameOptions(frame -> frame.sameOrigin()))
				.sessionManagement(session -> session.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
				.exceptionHandling(ex -> ex.authenticationEntryPoint(authenticationEntryPoint))
				.authorizeHttpRequests(auth -> auth
						.requestMatchers(HttpMethod.POST, "/api/v1/auth/**").permitAll()
						.requestMatchers(HttpMethod.POST, "/api/v1/users").permitAll()
						.requestMatchers(HttpMethod.GET, "/api/v1/users/*/subscription-requests").authenticated()
						.requestMatchers(HttpMethod.GET, "/api/v1/users/*/subscription-status").authenticated()
						.requestMatchers(HttpMethod.GET, "/api/v1/users/**").permitAll()
						.requestMatchers(HttpMethod.GET, "/api/v1/videos/**").permitAll()
						.requestMatchers(HttpMethod.GET, "/api/v1/posts/**").permitAll()
						.anyRequest().authenticated())
				.addFilterBefore(jwtAuthFilter, UsernamePasswordAuthenticationFilter.class);
		return http.build();
	}

	@Bean
	public BCryptPasswordEncoder passwordEncoder() {
		return new BCryptPasswordEncoder();
	}
}
