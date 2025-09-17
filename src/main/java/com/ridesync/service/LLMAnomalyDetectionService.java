package com.ridesync.service;

import java.util.Map;
import java.util.UUID;

/**
 * Service interface for LLM-based anomaly detection
 * Analyzes ride data using AI to detect complex anomalies
 */
public interface LLMAnomalyDetectionService {
    
    /**
     * Analyze ride data for anomalies using LLM
     * @param rideId The ride ID to analyze
     * @param userId The user ID whose location triggered the analysis
     * @return Map containing analysis results and detected anomalies
     */
    Map<String, Object> analyzeRideData(UUID rideId, UUID userId);
    
    /**
     * Check if LLM analysis is available (rate limiting, service status)
     * @param userId The user ID to check
     * @return true if analysis can be performed, false otherwise
     */
    boolean canAnalyze(UUID userId);
    
    /**
     * Get the last analysis time for a user (for rate limiting)
     * @param userId The user ID
     * @return Timestamp of last analysis, or null if never analyzed
     */
    Long getLastAnalysisTime(UUID userId);
    
    /**
     * Get supported anomaly types by this LLM service
     * @return Array of supported anomaly types
     */
    String[] getSupportedAnomalyTypes();
}
