package com.ridesync.service.impl;

import com.ridesync.model.Alert;
import com.ridesync.model.AlertType;
import com.ridesync.model.LocationUpdate;
import com.ridesync.repository.AlertRepository;
import com.ridesync.repository.LocationUpdateRepository;
import com.ridesync.service.AnomalyDetectionService;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;

@Service
@RequiredArgsConstructor
public class AnomalyDetectionServiceImpl implements AnomalyDetectionService {
    
    private static final Logger logger = LoggerFactory.getLogger(AnomalyDetectionServiceImpl.class);
    
    private final AlertRepository alertRepository;
    private final LocationUpdateRepository locationUpdateRepository;
    private final SimpMessagingTemplate messagingTemplate;
    
    // BUG T20: Hardcoded stationary threshold
    // @Value("${ridesync.stationary.threshold:180}")
    private int stationaryThresholdSeconds=180;
    
    // BUG T18: Slow anomaly detection - blocking IO
    public void detectAnomalies(UUID rideId) {
        // BUG T18: Blocking database calls in main thread
        logger.info("Detecting anomalies for ride: {}", rideId);
        List<LocationUpdate> recentUpdates = locationUpdateRepository
                .findByRideIdOrderByTimestampDesc(rideId);
        
        if (recentUpdates.isEmpty()) {
            return;
        }
        
        // Process each user's location data
        recentUpdates.stream()
                .collect(java.util.stream.Collectors.groupingBy(update -> update.getUser().getId()))
                .forEach((userId, userUpdates) -> {
                    detectStationaryAnomaly(userId, userUpdates);
                    // detectDirectionDrift(userId, userUpdates);
                });
        logger.info(" Anomalies detected for ride: {}", rideId);
    }
    
    // BUG T10: Stationary alerts false positive due to GPS jitter
    public void detectStationaryAnomaly(UUID userId, List<LocationUpdate> updates) {
        logger.info("Detecting stationary anomaly for user: {}", userId);
        if (updates.size() < 2) return;
        
        // Get the most recent updates (first in the list since it's ordered by timestamp desc)
        LocationUpdate latest = updates.get(0);
        LocationUpdate previous = updates.get(1);
        
        // BUG T10: No GPS accuracy consideration
        double distance = calculateDistance(
            latest.getLatitude(), latest.getLongitude(),
            previous.getLatitude(), previous.getLongitude()
        );
        
        long timeDiff = java.time.Duration.between(previous.getTimestamp(), latest.getTimestamp()).getSeconds();
        
        // For testing: Lower threshold to trigger alerts more easily
        int testThreshold = 10; // 10 seconds for testing
        
        // BUG T10: Hardcoded threshold without GPS accuracy
        // if (distance < 5.0 && timeDiff > testThreshold) {
            logger.info("Stationary anomaly detected: distance={}, timeDiff={}", distance, timeDiff);
            createAlert(Alert.builder()
                    .ride(latest.getRide())
                    .user(latest.getUser())
                    .device(latest.getDevice())
                    .type(AlertType.STATIONARY)
                    .message(String.format("User %s has been stationary for %d seconds", userId, timeDiff))
                    .isRead(false)
                    .build());
        // } else {
            logger.info("No stationary anomaly: distance={}, timeDiff={}", distance, timeDiff);
        // }
    }
    
    // BUG T11: Direction drift miscalculation
    public void detectDirectionDrift(UUID userId, List<LocationUpdate> updates) {
        if (updates.size() < 3) return;
        
        LocationUpdate latest = updates.get(0);
        
        // BUG T11: Wrong base heading calculation
        double baseHeading = calculateBearing(
            updates.get(2).getLatitude(), updates.get(2).getLongitude(),
            updates.get(1).getLatitude(), updates.get(1).getLongitude()
        );
        
        double currentHeading = calculateBearing(
            updates.get(1).getLatitude(), updates.get(1).getLongitude(),
            latest.getLatitude(), latest.getLongitude()
        );
        
        // BUG T11: Incorrect angle difference calculation
        double headingDiff = Math.abs(currentHeading - baseHeading);
        if (headingDiff > 45.0) { // BUG T11: Wrong threshold
            createAlert(Alert.builder()
                    .ride(latest.getRide())
                    .user(latest.getUser())
                    .device(latest.getDevice())
                    .type(AlertType.DIRECTION_DRIFT)
                    .message(String.format("User %s has significant direction drift: %.1f degrees", userId, headingDiff))
                    .isRead(false)
                    .build());
        }
    }
    
    // BUG T12: Broken template string
    public void createAlert(Alert alert) {
        logger.info("Creating alert of type: {}", alert.getType());
        // BUG T12: Template string not properly formatted
        // String message = String.format(
        //     "User %s has been stationary for %d seconds"); // BUG T12: Broken template
        alert.setMessage(alert.getMessage());
        alert.setCreatedAt(LocalDateTime.now());
            
        Alert savedAlert = alertRepository.save(alert);
        logger.info("Alert created: {} - {}", savedAlert.getType(), savedAlert.getMessage());
        logger.info("Broadcasting alert to WebSocket clients");
        // Broadcast alert to WebSocket clients
        broadcastAlert(savedAlert);
    }
    
    private void broadcastAlert(Alert alert) {
        try {
            // Create alert data for WebSocket broadcast
            java.util.Map<String, Object> alertData = new java.util.HashMap<>();
            alertData.put("id", alert.getId());
            alertData.put("type", alert.getType());
            alertData.put("message", alert.getMessage());
            alertData.put("severity", alert.getSeverity());
            alertData.put("isRead", alert.getIsRead());
            alertData.put("createdAt", alert.getCreatedAt());
            
            // Safely access lazy-loaded entities
            try {
                alertData.put("rideId", alert.getRide().getId());
            } catch (Exception e) {
                logger.warn("Could not access ride ID: {}", e.getMessage());
                alertData.put("rideId", "unknown");
            }
            
            try {
                alertData.put("userId", alert.getUser().getId());
                alertData.put("userName", alert.getUser().getUsername());
            } catch (Exception e) {
                logger.warn("Could not access user info: {}", e.getMessage());
                alertData.put("userId", "unknown");
                alertData.put("userName", "unknown");
            }
            
            alertData.put("latitude", alert.getLatitude());
            alertData.put("longitude", alert.getLongitude());
            
            // Add device info if available
            if (alert.getDevice() != null) {
                try {
                    alertData.put("deviceId", alert.getDevice().getId());
                    alertData.put("deviceType", alert.getDevice().getDeviceType());
                } catch (Exception e) {
                    logger.warn("Could not access device info: {}", e.getMessage());
                    alertData.put("deviceId", "unknown");
                    alertData.put("deviceType", "unknown");
                }
            }
            
            logger.info("Alert data prepared: {}", alertData);
            
            // Broadcast to all connected WebSocket clients
            messagingTemplate.convertAndSend("/topic/alerts", alertData);
            
            logger.info("Alert broadcasted to WebSocket clients successfully");
            
        } catch (Exception e) {
            logger.error("Error broadcasting alert: {}", e.getMessage(), e);
        }
    }
    
    // BUG T18: Should be async but is blocking
    public CompletableFuture<Void> detectAnomaliesAsync(UUID rideId) {
        return CompletableFuture.runAsync(() -> {
            // Still blocking in async context
            detectAnomalies(rideId);
        });
    }
    
    private double calculateDistance(double lat1, double lon1, double lat2, double lon2) {
        // Simplified distance calculation - BUG T14: Inaccurate distance calculation
        return Math.sqrt(Math.pow(lat2 - lat1, 2) + Math.pow(lon2 - lon1, 2)) * 111000; // Rough meters
    }
    
    private double calculateBearing(double lat1, double lon1, double lat2, double lon2) {
        // BUG T15: Incorrect bearing calculation
        double deltaLon = Math.toRadians(lon2 - lon1);
        double lat1Rad = Math.toRadians(lat1);
        double lat2Rad = Math.toRadians(lat2);
        
        double y = Math.sin(deltaLon) * Math.cos(lat2Rad);
        double x = Math.cos(lat1Rad) * Math.sin(lat2Rad) - 
                   Math.sin(lat1Rad) * Math.cos(lat2Rad) * Math.cos(deltaLon);
        
        return (Math.toDegrees(Math.atan2(y, x)) + 360) % 360;
    }
}
