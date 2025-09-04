package com.ridesync.service;

import com.ridesync.model.RefreshToken;
import com.ridesync.model.User;
import com.ridesync.repository.RefreshTokenRepository;
import com.ridesync.exception.ResourceNotFoundException;
import com.ridesync.exception.BusinessLogicException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.UUID;

@Service
@RequiredArgsConstructor
@Slf4j
public class RefreshTokenService {
    
    private final RefreshTokenRepository refreshTokenRepository;
    
    @Value("${app.jwt.refresh-token.expiration:604800}") // 7 days default
    private Long refreshTokenExpirationMs;
    
    public RefreshToken createRefreshToken(User user) {
        // Revoke all existing tokens for this user
        refreshTokenRepository.revokeAllUserTokens(user);
        
        RefreshToken refreshToken = RefreshToken.builder()
                .token(UUID.randomUUID().toString())
                .user(user)
                .expiryDate(LocalDateTime.now().plusSeconds(refreshTokenExpirationMs))
                .isRevoked(false)
                .createdAt(LocalDateTime.now())
                .build();
        
        return refreshTokenRepository.save(refreshToken);
    }
    
    public RefreshToken findByToken(String token) {
        return refreshTokenRepository.findByToken(token)
                .orElseThrow(() -> new ResourceNotFoundException("Refresh token not found"));
    }
    
    public RefreshToken verifyExpiration(RefreshToken token) {
        if (token.getExpiryDate().isBefore(LocalDateTime.now())) {
            refreshTokenRepository.delete(token);
            throw new BusinessLogicException("Refresh token was expired. Please make a new signin request");
        }
        
        if (token.getIsRevoked()) {
            throw new BusinessLogicException("Refresh token was revoked. Please make a new signin request");
        }
        
        return token;
    }
    
    @Transactional
    public RefreshToken refreshToken(String token) {
        RefreshToken refreshToken = findByToken(token);
        verifyExpiration(refreshToken);
        
        // Update last used timestamp
        refreshToken.setLastUsedAt(LocalDateTime.now());
        refreshTokenRepository.updateLastUsed(token, LocalDateTime.now());
        
        return refreshToken;
    }
    
    @Transactional
    public void revokeToken(String token) {
        RefreshToken refreshToken = findByToken(token);
        refreshToken.setIsRevoked(true);
        refreshTokenRepository.save(refreshToken);
    }
    
    @Transactional
    public void revokeAllUserTokens(User user) {
        refreshTokenRepository.revokeAllUserTokens(user);
    }
    
    @Transactional
    public void deleteExpiredTokens() {
        refreshTokenRepository.deleteExpiredTokens(LocalDateTime.now());
    }
}
