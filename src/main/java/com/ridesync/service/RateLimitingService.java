package com.ridesync.service;

import org.springframework.stereotype.Service;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Service for rate limiting LLM API calls
 * Ensures max 1 call per minute per user
 */
@Service
public class RateLimitingService {
    
    private static final Logger logger = LoggerFactory.getLogger(RateLimitingService.class);
    
    // Store last analysis time per user
    private final Map<UUID, Long> lastAnalysisTimes = new ConcurrentHashMap<>();
    
    // Rate limit: 1 call per minute (60 seconds)
    private static final long RATE_LIMIT_INTERVAL_MS = 60 * 1000;
    
    /**
     * Check if user can make an LLM analysis call
     * @param userId The user ID to check
     * @return true if call is allowed, false if rate limited
     */
    public boolean canMakeCall(UUID userId) {
        Long lastCallTime = lastAnalysisTimes.get(userId);
        
        if (lastCallTime == null) {
            // First call for this user
            logger.debug("First LLM call for user: {}", userId);
            return true;
        }
        
        long currentTime = System.currentTimeMillis();
        long timeSinceLastCall = currentTime - lastCallTime;
        
        if (timeSinceLastCall >= RATE_LIMIT_INTERVAL_MS) {
            logger.debug("LLM call allowed for user: {} ({}ms since last call)", userId, timeSinceLastCall);
            return true;
        } else {
            long remainingTime = RATE_LIMIT_INTERVAL_MS - timeSinceLastCall;
            logger.debug("LLM call rate limited for user: {} ({}ms remaining)", userId, remainingTime);
            return false;
        }
    }
    
    /**
     * Record that an LLM analysis call was made for a user
     * @param userId The user ID
     */
    public void recordCall(UUID userId) {
        long currentTime = System.currentTimeMillis();
        lastAnalysisTimes.put(userId, currentTime);
        logger.debug("Recorded LLM call for user: {} at {}", userId, currentTime);
    }
    
    /**
     * Get the last analysis time for a user
     * @param userId The user ID
     * @return Timestamp of last analysis, or null if never analyzed
     */
    public Long getLastAnalysisTime(UUID userId) {
        return lastAnalysisTimes.get(userId);
    }
    
    /**
     * Get remaining time until next call is allowed
     * @param userId The user ID
     * @return Remaining time in milliseconds, or 0 if call is allowed
     */
    public long getRemainingTime(UUID userId) {
        Long lastCallTime = lastAnalysisTimes.get(userId);
        
        if (lastCallTime == null) {
            return 0;
        }
        
        long currentTime = System.currentTimeMillis();
        long timeSinceLastCall = currentTime - lastCallTime;
        
        if (timeSinceLastCall >= RATE_LIMIT_INTERVAL_MS) {
            return 0;
        } else {
            return RATE_LIMIT_INTERVAL_MS - timeSinceLastCall;
        }
    }
    
    /**
     * Clear rate limiting data for a user (for testing)
     * @param userId The user ID
     */
    public void clearUserData(UUID userId) {
        lastAnalysisTimes.remove(userId);
        logger.debug("Cleared rate limiting data for user: {}", userId);
    }
}
