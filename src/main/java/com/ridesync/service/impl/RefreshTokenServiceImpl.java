package com.ridesync.service.impl;

import com.ridesync.exception.ResourceNotFoundException;
import com.ridesync.model.RefreshToken;
import com.ridesync.model.User;
import com.ridesync.repository.RefreshTokenRepository;
import com.ridesync.service.RefreshTokenService;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.Optional;
import java.util.UUID;

@Service
@Transactional
@RequiredArgsConstructor
public class RefreshTokenServiceImpl implements RefreshTokenService {
    
    private final RefreshTokenRepository refreshTokenRepository;
    
    @Value("${app.jwt.refresh-token-expiration:604800000}") // 7 days in milliseconds
    private long refreshTokenExpirationMs;
    
    public RefreshToken createRefreshToken(User user) {
        // Revoke all existing tokens for this user
        refreshTokenRepository.revokeAllUserTokens(user);
        
        RefreshToken refreshToken = RefreshToken.builder()
                .token(UUID.randomUUID().toString())
                .user(user)
                .expiryDate(LocalDateTime.now().plusSeconds(refreshTokenExpirationMs))
                .isRevoked(false)
                .build();
        
        return refreshTokenRepository.save(refreshToken);
    }
    
    public Optional<RefreshToken> findByToken(String token) {
        return refreshTokenRepository.findByToken(token);
    }
    
    public RefreshToken refreshToken(String token) {
        RefreshToken refreshToken = refreshTokenRepository.findByToken(token)
                .orElseThrow(() -> new ResourceNotFoundException("RefreshToken", "token", token));
        
        if (!isTokenValid(refreshToken)) {
            throw new RuntimeException("Refresh token is invalid or expired");
        }
        
        // Generate new access token (this would be done by JwtUtil in real implementation)
        return refreshToken;
    }
    
    public void revokeToken(String token) {
        RefreshToken refreshToken = refreshTokenRepository.findByToken(token)
                .orElseThrow(() -> new ResourceNotFoundException("RefreshToken", "token", token));
        
        refreshToken.setIsRevoked(true);
        refreshToken.setUpdatedAt(LocalDateTime.now());
        refreshTokenRepository.save(refreshToken);
    }
    
    public void revokeAllUserTokens(User user) {
        refreshTokenRepository.revokeAllUserTokens(user);
    }
    
    public boolean isTokenValid(RefreshToken token) {
        return token != null && 
               !token.getIsRevoked() && 
               token.getExpiryDate().isAfter(LocalDateTime.now());
    }
}
