package com.ridesync.config;

import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;

import java.time.Duration;
import java.util.UUID;

@Service
public class SessionManager {
    
    private final RedisTemplate<String, String> redisTemplate;
    private static final String SESSION_KEY_PREFIX = "active_session:";
    private static final Duration SESSION_TIMEOUT = Duration.ofMinutes(30);
    
    public SessionManager(RedisTemplate<String, String> redisTemplate) {
        this.redisTemplate = redisTemplate;
    }
    
    public boolean hasActiveSession(UUID userId, UUID rideId) {
        String key = SESSION_KEY_PREFIX + userId + ":" + rideId;
        return Boolean.TRUE.equals(redisTemplate.hasKey(key));
    }
    
    public void createSession(UUID userId, UUID rideId) {
        String key = SESSION_KEY_PREFIX + userId + ":" + rideId;
        redisTemplate.opsForValue().set(key, "active", SESSION_TIMEOUT);
    }
    
    public void createOrUpdateSession(UUID userId, UUID rideId) {
        String key = SESSION_KEY_PREFIX + userId + ":" + rideId;
        // This will create a new session or update the existing one with a new timeout
        redisTemplate.opsForValue().set(key, "active", SESSION_TIMEOUT);
    }
    
    public void endSession(UUID userId, UUID rideId) {
        String key = SESSION_KEY_PREFIX + userId + ":" + rideId;
        redisTemplate.delete(key);
    }
    
    public void endAllUserSessions(UUID userId) {
        String pattern = SESSION_KEY_PREFIX + userId + ":*";
        redisTemplate.delete(redisTemplate.keys(pattern));
    }
}
