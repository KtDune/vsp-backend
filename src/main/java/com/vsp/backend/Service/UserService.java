package com.vsp.backend.Service;

import com.vsp.backend.Dto.UserResponse;
import com.vsp.backend.Model.User;
import com.vsp.backend.Repository.UserRepository;

import java.util.Map;
import java.util.Optional;
import java.util.regex.Pattern;

import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class UserService {

	private final UserRepository userRepository;
	private final BCryptPasswordEncoder passwordEncoder;

	public UserService(UserRepository userRepository, BCryptPasswordEncoder passwordEncoder) {
		this.userRepository = userRepository;
		this.passwordEncoder = passwordEncoder;
	}

	private static final Pattern USERNAME_PATTERN = Pattern.compile("^[a-zA-Z0-9_]{5,20}$");
	private static final Pattern EMAIL_PATTERN = Pattern.compile("^[^\\s@]+@[^\\s@]+\\.[^\\s@]+$");
	private static final Pattern PASSWORD_UPPERCASE = Pattern.compile(".*[A-Z].*");
	private static final Pattern PASSWORD_NUMBER = Pattern.compile(".*[0-9].*");
	private static final Pattern PASSWORD_SYMBOL = Pattern.compile(".*[^a-zA-Z0-9].*");

	public Map<String, Object> create(User user) {
		if (user.getUsername() == null || !USERNAME_PATTERN.matcher(user.getUsername()).matches()) {
			return Map.of("success", false, "message", "Username must be 5-20 characters (letters, digits, _ only)");
		}
		if (user.getEmail() == null) {
			return Map.of("success", false, "message", "Email is required");
		}
		String email = user.getEmail().toLowerCase();
		if (email.length() < 5 || email.length() > 50 || !EMAIL_PATTERN.matcher(email).matches()) {
			return Map.of("success", false, "message", "Email must be a valid address (5-50 characters)");
		}
		String password = user.getPassword();
		if (password == null || password.length() < 8 || password.length() > 50
				|| !PASSWORD_UPPERCASE.matcher(password).find() || !PASSWORD_NUMBER.matcher(password).find()
				|| !PASSWORD_SYMBOL.matcher(password).find()) {
			return Map.of("success", false, "message",
					"Password must be 8-50 characters with at least one uppercase letter, one number, and one symbol");
		}
		if (user.getReEnterPassword() == null || !user.getReEnterPassword().equals(password)) {
			return Map.of("success", false, "message", "Passwords do not match");
		}
		if (user.getProfileUrl() != null && user.getProfileUrl().length() > 255) {
			return Map.of("success", false, "message", "Profile URL must be 255 characters or fewer");
		}
		if (userRepository.existsByEmailIgnoreCase(email)) {
			return Map.of("success", false, "message", "Email already in use");
		}
		if (userRepository.existsByUsernameIgnoreCase(user.getUsername())) {
			return Map.of("success", false, "message", "Username already in use");
		}

		user.setId(null);
		user.setEmail(email);
		user.setPasswordHash(passwordEncoder.encode(password));
		user.setPassword(null);
		user.setReEnterPassword(null);
		if (user.getVisibility() == null) {
			user.setVisibility(User.Visibility.PUBLIC);
		}
		user.setIsActive(true);

		User saved = userRepository.save(user);
		return Map.of("success", true, "message", "User created", "id", saved.getId());
	}

	@Transactional
	public Map<String, Object> updateById(Long id, User changes) {
		Optional<User> result = userRepository.findByIdAndIsActiveTrue(id);

		if (result.isEmpty()) {
			return Map.of("success", false, "message", "User not found");
		}

		User user = result.get();

		if (changes.getEmail() != null) {
			return Map.of("success", false, "message", "Email is not editable");
		}

		if (changes.getUsername() != null && !changes.getUsername().equals(user.getUsername())) {
			if (userRepository.existsByUsernameIgnoreCase(changes.getUsername())) {
				return Map.of("success", false, "message", "Username already in use");
			}
			user.setUsername(changes.getUsername());
		}

		if (changes.getPassword() != null) {
			user.setPasswordHash(passwordEncoder.encode(changes.getPassword()));
		}

		if (changes.getVisibility() != null) {
			user.setVisibility(changes.getVisibility());
		}

		if (changes.getProfileUrl() != null) {
			if (changes.getProfileUrl().length() > 255) {
				return Map.of("success", false, "message", "Profile URL must be 255 characters or fewer");
			}
			user.setProfileUrl(changes.getProfileUrl());
		}

		userRepository.save(user);
		return Map.of("success", true, "message", "User updated");
	}

	@Transactional
	public boolean deactivateById(Long id, User request) {
		Optional<User> result = userRepository.findByIdAndIsActiveTrue(id);

		if (result.isEmpty()) {
			return false;
		}

		User user = result.get();

		if (request.getPassword() == null
				|| !passwordEncoder.matches(request.getPassword(), user.getPasswordHash())) {
			return false;
		}

		user.setUsername("deactivateduser_" + id);
		user.setEmail("deleted_" + id + "@anonymized.invalid");
		user.setIsActive(false);
		userRepository.save(user);
		return true;
	}

	public UserResponse getById(Long id) {
		Optional<User> result = userRepository.findByIdAndIsActiveTrue(id);

		if (result.isEmpty()) {
			return null;
		}

		User user = result.get();

		if (user.getVisibility() == User.Visibility.PRIVATE) {
			return UserResponse.publicView(user);
		}

		return UserResponse.fullView(user);
	}
}
