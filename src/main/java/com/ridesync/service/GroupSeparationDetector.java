package com.ridesync.service;

import java.util.List;
import java.util.Map;
import java.util.UUID;

/**
 * Service for detecting group separation based on radius/distance
 */
public interface GroupSeparationDetector {
    
    /**
     * Detect group separation for a specific ride
     * @param rideId The ride ID to analyze
     * @return Map containing separation analysis results
     */
    Map<String, Object> detectGroupSeparation(UUID rideId);
    
    /**
     * Check if radius-based alerts are enabled
     * @return true if enabled, false otherwise
     */
    boolean isRadiusAlertsEnabled();
    
    /**
     * Get the warning distance threshold in meters
     * @return warning distance in meters
     */
    int getWarningDistanceMeters();
    
    /**
     * Get the critical distance threshold in meters
     * @return critical distance in meters
     */
    int getCriticalDistanceMeters();
}
