package com.guerrero.timeproject.controller;

import com.guerrero.timeproject.dto.AuthResponse;
import com.guerrero.timeproject.dto.LoginRequest;
import com.guerrero.timeproject.dto.RegisterRequest;
import com.guerrero.timeproject.model.Role;
import com.guerrero.timeproject.model.User;
import com.guerrero.timeproject.repository.UserRepository;
import com.guerrero.timeproject.security.JwtTokenProvider;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/auth")
@RequiredArgsConstructor
public class AuthController {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtTokenProvider tokenProvider;

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

        // Generate JWT token
        String jwt = tokenProvider.generateToken(savedUser.getUsername());

        return ResponseEntity.status(HttpStatus.CREATED).body(
                AuthResponse.builder()
                        .token(jwt)
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

        // Generate JWT token
        String jwt = tokenProvider.generateToken(user.getUsername());

        return ResponseEntity.ok(
                AuthResponse.builder()
                        .token(jwt)
                        .userId(user.getId())
                        .username(user.getUsername())
                        .role(user.getRole().name())
                        .build()
        );
    }
}
