package com.ridesync.service.impl;

import com.ridesync.model.LocationUpdate;
import com.ridesync.model.Ride;
import com.ridesync.model.User;
import com.ridesync.repository.LocationUpdateRepository;
import com.ridesync.repository.RideRepository;
import com.ridesync.service.GroupSeparationDetector;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.*;
import java.util.stream.Collectors;

/**
 * Implementation of group separation detection based on radius/distance
 */
@Service
@RequiredArgsConstructor
public class GroupSeparationDetectorImpl implements GroupSeparationDetector {

    private static final Logger logger = LoggerFactory.getLogger(GroupSeparationDetectorImpl.class);

    private final LocationUpdateRepository locationUpdateRepository;
    private final RideRepository rideRepository;

    @Value("${ridesync.alerts.radius.enabled:true}")
    private boolean radiusAlertsEnabled;

    @Value("${ridesync.alerts.radius.warning-distance-meters:2000}")
    private int warningDistanceMeters;

    @Value("${ridesync.alerts.radius.critical-distance-meters:5000}")
    private int criticalDistanceMeters;

    @Value("${ridesync.alerts.radius.min-group-size:1}")
    private int minGroupSize;

    @Value("${ridesync.alerts.radius.location-timeout-minutes:5}")
    private int locationTimeoutMinutes;

    @Override
    public Map<String, Object> detectGroupSeparation(UUID rideId) {
        Map<String, Object> result = new HashMap<>();
        
        try {
            logger.info("Starting group separation detection for ride: {}", rideId);
            
            if (!radiusAlertsEnabled) {
                result.put("status", "disabled");
                result.put("message", "Radius-based alerts are disabled");
                return result;
            }

            // Get ride information
            Optional<Ride> rideOpt = rideRepository.findById(rideId);
            if (rideOpt.isEmpty()) {
                result.put("status", "error");
                result.put("message", "Ride not found");
                return result;
            }

            Ride ride = rideOpt.get();
            
            // Get recent location updates for all group members
            List<LocationUpdate> recentLocations = getRecentGroupLocations(rideId);
            
            if (recentLocations.size() < minGroupSize) {
                result.put("status", "insufficient_data");
                result.put("message", "Not enough group members with recent location data");
                result.put("memberCount", recentLocations.size());
                result.put("minRequired", minGroupSize);
                return result;
            }

            // Calculate group centroid
            Map<String, Double> centroid = calculateGroupCentroid(recentLocations);
            
            // Analyze separation for each user
            List<Map<String, Object>> separationAnalysis = analyzeUserSeparation(recentLocations, centroid);
            
            // Generate alerts for separated users
            List<Map<String, Object>> alerts = generateSeparationAlerts(separationAnalysis, rideId);
            
            result.put("status", "success");
            result.put("rideId", rideId.toString());
            result.put("centroid", centroid);
            result.put("totalMembers", recentLocations.size());
            result.put("separationAnalysis", separationAnalysis);
            result.put("alerts", alerts);
            result.put("timestamp", LocalDateTime.now().toString());
            
            logger.info("Group separation detection completed for ride: {}, found {} alerts", 
                       rideId, alerts.size());
            
        } catch (Exception e) {
            logger.error("Error in group separation detection for ride: {} - {}", rideId, e.getMessage(), e);
            result.put("status", "error");
            result.put("message", e.getMessage());
        }
        
        return result;
    }

    @Override
    public boolean isRadiusAlertsEnabled() {
        return radiusAlertsEnabled;
    }

    @Override
    public int getWarningDistanceMeters() {
        return warningDistanceMeters;
    }

    @Override
    public int getCriticalDistanceMeters() {
        return criticalDistanceMeters;
    }

    /**
     * Get recent location updates for all group members
     */
    private List<LocationUpdate> getRecentGroupLocations(UUID rideId) {
        // LocalDateTime cutoffTime = LocalDateTime.now().minusMinutes(locationTimeoutMinutes);
        
        return locationUpdateRepository.findByRideIdOrderByTimestampDesc(rideId)
                .stream()
                .collect(Collectors.groupingBy(LocationUpdate::getUser))
                .values()
                .stream()
                .map(userLocations -> userLocations.stream()
                        .max(Comparator.comparing(LocationUpdate::getTimestamp))
                        .orElse(null))
                .filter(Objects::nonNull)
                .collect(Collectors.toList());
    }

    /**
     * Calculate the group centroid (center point)
     */
    private Map<String, Double> calculateGroupCentroid(List<LocationUpdate> locations) {
        if (locations.isEmpty()) {
            return Map.of("latitude", 0.0, "longitude", 0.0);
        }

        double avgLat = locations.stream()
                .mapToDouble(LocationUpdate::getLatitude)
                .average()
                .orElse(0.0);

        double avgLon = locations.stream()
                .mapToDouble(LocationUpdate::getLongitude)
                .average()
                .orElse(0.0);

        Map<String, Double> centroid = new HashMap<>();
        centroid.put("latitude", avgLat);
        centroid.put("longitude", avgLon);
        
        logger.debug("Group centroid calculated: lat={}, lon={}", avgLat, avgLon);
        return centroid;
    }

    /**
     * Analyze separation for each user from the group centroid
     */
    private List<Map<String, Object>> analyzeUserSeparation(List<LocationUpdate> locations, Map<String, Double> centroid) {
        List<Map<String, Object>> analysis = new ArrayList<>();
        
        double centroidLat = centroid.get("latitude");
        double centroidLon = centroid.get("longitude");
        
        for (LocationUpdate location : locations) {
            double distance = calculateDistance(
                location.getLatitude(), location.getLongitude(),
                centroidLat, centroidLon
            );
            
            Map<String, Object> userAnalysis = new HashMap<>();
            userAnalysis.put("userId", location.getUser().getId().toString());
            userAnalysis.put("userName", location.getUser().getUsername());
            userAnalysis.put("latitude", location.getLatitude());
            userAnalysis.put("longitude", location.getLongitude());
            userAnalysis.put("distanceFromCentroid", distance);
            userAnalysis.put("timestamp", location.getTimestamp());
            
            // Determine separation level
            String separationLevel = "NORMAL";
            if (distance >= criticalDistanceMeters) {
                separationLevel = "CRITICAL";
            } else if (distance >= warningDistanceMeters) {
                separationLevel = "WARNING";
            }
            userAnalysis.put("separationLevel", separationLevel);
            
            analysis.add(userAnalysis);
        }
        
        return analysis;
    }

    /**
     * Generate alerts for users who are separated from the group
     */
    private List<Map<String, Object>> generateSeparationAlerts(List<Map<String, Object>> analysis, UUID rideId) {
        List<Map<String, Object>> alerts = new ArrayList<>();
        
        for (Map<String, Object> userAnalysis : analysis) {
            String separationLevel = (String) userAnalysis.get("separationLevel");
            double distance = (Double) userAnalysis.get("distanceFromCentroid");
            
            if (!"NORMAL".equals(separationLevel)) {
                Map<String, Object> alert = new HashMap<>();
                alert.put("type", "GROUP_SEPARATION");
                alert.put("severity", separationLevel.equals("CRITICAL") ? "HIGH" : "MEDIUM");
                alert.put("rideId", rideId.toString());
                alert.put("userId", userAnalysis.get("userId"));
                alert.put("userName", userAnalysis.get("userName"));
                alert.put("distance", distance);
                alert.put("distanceFormatted", formatDistance(distance));
                alert.put("threshold", separationLevel.equals("CRITICAL") ? criticalDistanceMeters : warningDistanceMeters);
                alert.put("message", createSeparationMessage(userAnalysis, separationLevel));
                alert.put("timestamp", LocalDateTime.now().toString());
                alert.put("source", "radius_detector");
                
                alerts.add(alert);
            }
        }
        
        return alerts;
    }

    /**
     * Calculate distance between two points using Haversine formula
     */
    private double calculateDistance(double lat1, double lon1, double lat2, double lon2) {
        final int R = 6371000; // Earth's radius in meters
        
        double lat1Rad = Math.toRadians(lat1);
        double lat2Rad = Math.toRadians(lat2);
        double deltaLatRad = Math.toRadians(lat2 - lat1);
        double deltaLonRad = Math.toRadians(lon2 - lon1);
        
        double a = Math.sin(deltaLatRad / 2) * Math.sin(deltaLatRad / 2) +
                  Math.cos(lat1Rad) * Math.cos(lat2Rad) *
                  Math.sin(deltaLonRad / 2) * Math.sin(deltaLonRad / 2);
        
        double c = 2 * Math.atan2(Math.sqrt(a), Math.sqrt(1 - a));
        
        return R * c; // Distance in meters
    }

    /**
     * Format distance for human-readable display
     */
    private String formatDistance(double distanceMeters) {
        if (distanceMeters < 1000) {
            return String.format("%.0f m", distanceMeters);
        } else {
            return String.format("%.1f km", distanceMeters / 1000.0);
        }
    }

    /**
     * Create separation alert message
     */
    private String createSeparationMessage(Map<String, Object> userAnalysis, String separationLevel) {
        String userName = (String) userAnalysis.get("userName");
        String distanceFormatted = formatDistance((Double) userAnalysis.get("distanceFromCentroid"));
        
        if ("CRITICAL".equals(separationLevel)) {
            return String.format("🚨 CRITICAL: %s is %s away from the group! Immediate attention required.", 
                               userName, distanceFormatted);
        } else {
            return String.format("⚠️ WARNING: %s is %s away from the group. Please check on them.", 
                               userName, distanceFormatted);
        }
    }
}
