package com.chessnalysis.service.auth;

import com.chessnalysis.dao.user.UserRepository;
import com.chessnalysis.domain.user.User;
import com.chessnalysis.domain.user.UserRole;
import com.chessnalysis.dto.auth.LoginRequest;
import com.chessnalysis.dto.auth.LoginResponse;
import com.chessnalysis.dto.auth.RegisterRequest;
import com.chessnalysis.dto.auth.RegisterResponse;
import com.chessnalysis.exception.AuthenticationException;
import com.chessnalysis.exception.ResourceAlreadyExistsException;
import com.chessnalysis.security.jwt.JwtProvider;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Service for handling user authentication and registration.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class AuthService {

	private final UserRepository userRepository;
	private final PasswordEncoder passwordEncoder;
	private final JwtProvider jwtProvider;

	/**
	 * Register a new user.
	 */
	@Transactional
	public RegisterResponse register(RegisterRequest request) {
		if (userRepository.existsByUsername(request.username())) {
			throw new ResourceAlreadyExistsException("Username already exists: " + request.username());
		}

		User user = User.builder()
			.username(request.username())
			.passwordHash(passwordEncoder.encode(request.password()))
			.role(UserRole.USER)
			.build();

		user = userRepository.save(user);
		log.info("User registered successfully: {}", user.getUsername());

		return new RegisterResponse(
			user.getId(),
			user.getUsername(),
			"Registration successful"
		);
	}

	/**
	 * Authenticate user and return JWT token.
	 */
	@Transactional(readOnly = true)
	public LoginResponse login(LoginRequest request) {
		User user = userRepository.findByUsername(request.username())
			.orElseThrow(() -> new AuthenticationException("Invalid username or password"));

		if (!passwordEncoder.matches(request.password(), user.getPasswordHash())) {
			throw new AuthenticationException("Invalid username or password");
		}

		String token = jwtProvider.generateToken(user);
		log.info("User logged in successfully: {}", user.getUsername());

		return new LoginResponse(
			token,
			user.getId(),
			user.getUsername(),
			user.getRole().name()
		);
	}
}

