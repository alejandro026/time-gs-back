package com.guerrero.timeproject.controller;

import com.guerrero.timeproject.dto.AuthResponse;
import com.guerrero.timeproject.dto.LoginRequest;
import com.guerrero.timeproject.dto.RegisterRequest;
import com.guerrero.timeproject.dto.TokenRefreshRequest;
import com.guerrero.timeproject.dto.TokenRefreshResponse;
import com.guerrero.timeproject.model.RefreshToken;
import com.guerrero.timeproject.model.Role;
import com.guerrero.timeproject.model.User;
import com.guerrero.timeproject.repository.UserRepository;
import com.guerrero.timeproject.security.JwtTokenProvider;
import com.guerrero.timeproject.service.RefreshTokenService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.web.bind.annotation.*;
import java.security.Principal;

@RestController
@RequestMapping("/api/auth")
@RequiredArgsConstructor
public class AuthController {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtTokenProvider tokenProvider;
    private final RefreshTokenService refreshTokenService;

    @PostMapping("/register")
    public ResponseEntity<?> registerUser(@Valid @RequestBody RegisterRequest registerRequest) {
        if (userRepository.existsByUsername(registerRequest.getUsername())) {
            return ResponseEntity
                    .status(HttpStatus.BAD_REQUEST)
                    .body("Username is already taken!");
        }

        // Create new user's account
        User user = User.builder()
                .username(registerRequest.getUsername())
                .password(passwordEncoder.encode(registerRequest.getPassword()))
                .email(registerRequest.getEmail())
                .role(Role.ROLE_USER)
                .build();

        User savedUser = userRepository.save(user);

        // Generate access token & refresh token
        String jwt = tokenProvider.generateToken(savedUser.getUsername());
        RefreshToken refreshToken = refreshTokenService.createRefreshToken(savedUser.getUsername());

        return ResponseEntity.status(HttpStatus.CREATED).body(
                AuthResponse.builder()
                        .token(jwt)
                        .refreshToken(refreshToken.getToken())
                        .userId(savedUser.getId())
                        .username(savedUser.getUsername())
                        .role(savedUser.getRole().name())
                        .build()
        );
    }

    @PostMapping("/login")
    public ResponseEntity<?> authenticateUser(@Valid @RequestBody LoginRequest loginRequest) {
        User user = userRepository.findByUsername(loginRequest.getUsername())
                .orElse(null);

        if (user == null || !passwordEncoder.matches(loginRequest.getPassword(), user.getPassword())) {
            return ResponseEntity
                    .status(HttpStatus.UNAUTHORIZED)
                    .body("Invalid username or password!");
        }

        // Generate access token & refresh token
        String jwt = tokenProvider.generateToken(user.getUsername());
        RefreshToken refreshToken = refreshTokenService.createRefreshToken(user.getUsername());

        return ResponseEntity.ok(
                AuthResponse.builder()
                        .token(jwt)
                        .refreshToken(refreshToken.getToken())
                        .userId(user.getId())
                        .username(user.getUsername())
                        .role(user.getRole().name())
                        .build()
        );
    }

    @PostMapping("/refresh")
    public ResponseEntity<?> refreshAccessToken(@Valid @RequestBody TokenRefreshRequest request) {
        try {
            TokenRefreshResponse response = refreshTokenService.rotateRefreshToken(request.getRefreshToken());
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN).body(e.getMessage());
        }
    }

    @PostMapping("/logout")
    public ResponseEntity<?> logoutUser(Principal principal) {
        if (principal != null) {
            refreshTokenService.revokeAllByUser(principal.getName());
        }
        return ResponseEntity.ok("Successfully logged out!");
    }
}
