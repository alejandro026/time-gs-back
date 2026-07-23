package com.guerrero.timeproject.service;

import com.guerrero.timeproject.model.RefreshToken;
import com.guerrero.timeproject.dto.TokenRefreshResponse;

public interface RefreshTokenService {
    RefreshToken createRefreshToken(String username);
    RefreshToken verifyExpiration(RefreshToken token);
    TokenRefreshResponse rotateRefreshToken(String requestToken);
    void revokeAllByUser(String username);
}
