package com.ridesync.service;

import com.ridesync.model.RefreshToken;
import com.ridesync.model.User;

import java.util.Optional;

public interface RefreshTokenService {
    
    RefreshToken createRefreshToken(User user);
    
    Optional<RefreshToken> findByToken(String token);
    
    RefreshToken refreshToken(String token);
    
    void revokeToken(String token);
    
    void revokeAllUserTokens(User user);
    
    boolean isTokenValid(RefreshToken token);
}