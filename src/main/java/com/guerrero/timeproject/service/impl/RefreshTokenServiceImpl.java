package com.guerrero.timeproject.service.impl;

import com.guerrero.timeproject.model.RefreshToken;
import com.guerrero.timeproject.model.User;
import com.guerrero.timeproject.dto.TokenRefreshResponse;
import com.guerrero.timeproject.repository.RefreshTokenRepository;
import com.guerrero.timeproject.repository.UserRepository;
import com.guerrero.timeproject.security.JwtTokenProvider;
import com.guerrero.timeproject.service.RefreshTokenService;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.time.Instant;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class RefreshTokenServiceImpl implements RefreshTokenService {

    @Value("${app.jwt.refresh-expiration-ms:604800000}") // 7 days default
    private long refreshExpirationMs;

    private final RefreshTokenRepository refreshTokenRepository;
    private final UserRepository userRepository;
    private final JwtTokenProvider tokenProvider;

    @Override
    @Transactional
    public RefreshToken createRefreshToken(String username) {
        User user = userRepository.findByUsername(username)
                .orElseThrow(() -> new RuntimeException("User not found: " + username));

        RefreshToken refreshToken = RefreshToken.builder()
                .user(user)
                .token(UUID.randomUUID().toString())
                .expiryDate(Instant.now().plusMillis(refreshExpirationMs))
                .revoked(false)
                .build();

        return refreshTokenRepository.save(refreshToken);
    }

    @Override
    @Transactional
    public RefreshToken verifyExpiration(RefreshToken token) {
        if (token.getExpiryDate().isBefore(Instant.now())) {
            refreshTokenRepository.delete(token);
            throw new RuntimeException("Refresh token has expired! Please log in again.");
        }
        return token;
    }

    @Override
    @Transactional
    public TokenRefreshResponse rotateRefreshToken(String requestToken) {
        RefreshToken oldToken = refreshTokenRepository.findByToken(requestToken)
                .orElseThrow(() -> new RuntimeException("Refresh token not found in database!"));

        User user = oldToken.getUser();

        // 1. REUSE DETECTION (Rotation Security)
        if (oldToken.isRevoked()) {
            // An already revoked token is used -> potential attack!
            // Revoke ALL tokens belonging to this user for safety
            refreshTokenRepository.revokeAllByUser(user);
            throw new RuntimeException("Security Alert: Refresh token reuse detected! All user sessions revoked.");
        }

        // 2. Expiration Verification
        verifyExpiration(oldToken);

        // 3. Mark old token as used/revoked
        String newRefreshTokenString = UUID.randomUUID().toString();
        oldToken.setRevoked(true);
        oldToken.setReplacedBy(newRefreshTokenString);
        refreshTokenRepository.save(oldToken);

        // 4. Create new rotated Refresh Token
        RefreshToken newRefreshToken = RefreshToken.builder()
                .user(user)
                .token(newRefreshTokenString)
                .expiryDate(Instant.now().plusMillis(refreshExpirationMs))
                .revoked(false)
                .build();
        refreshTokenRepository.save(newRefreshToken);

        // 5. Generate new Access Token
        String newAccessToken = tokenProvider.generateToken(user.getUsername());

        return TokenRefreshResponse.builder()
                .accessToken(newAccessToken)
                .refreshToken(newRefreshTokenString)
                .build();
    }

    @Override
    @Transactional
    public void revokeAllByUser(String username) {
        User user = userRepository.findByUsername(username)
                .orElseThrow(() -> new RuntimeException("User not found: " + username));
        // Delete all user tokens to clean up the DB
        refreshTokenRepository.deleteByUser(user);
    }
}
