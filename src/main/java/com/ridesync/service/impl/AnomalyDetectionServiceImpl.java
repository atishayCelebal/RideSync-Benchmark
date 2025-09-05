package com.ridesync.service.impl;

import com.ridesync.model.Alert;
import com.ridesync.model.AlertType;
import com.ridesync.model.LocationUpdate;
import com.ridesync.repository.AlertRepository;
import com.ridesync.repository.LocationUpdateRepository;
import com.ridesync.service.AnomalyDetectionService;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;

@Service
@RequiredArgsConstructor
public class AnomalyDetectionServiceImpl implements AnomalyDetectionService {
    
    private final AlertRepository alertRepository;
    private final LocationUpdateRepository locationUpdateRepository;
    
    // BUG T20: Hardcoded stationary threshold
    // @Value("${ridesync.stationary.threshold:180}")
    private int stationaryThresholdSeconds=180;
    
    // BUG T18: Slow anomaly detection - blocking IO
    public void detectAnomalies(UUID rideId) {
        // BUG T18: Blocking database calls in main thread
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
                    detectDirectionDrift(userId, userUpdates);
                });
    }
    
    // BUG T10: Stationary alerts false positive due to GPS jitter
    public void detectStationaryAnomaly(UUID userId, List<LocationUpdate> updates) {
        if (updates.size() < 2) return;
        
        LocationUpdate latest = updates.get(0);
        LocationUpdate previous = updates.get(1);
        
        // BUG T10: No GPS accuracy consideration
        double distance = calculateDistance(
            latest.getLatitude(), latest.getLongitude(),
            previous.getLatitude(), previous.getLongitude()
        );
        
        long timeDiff = java.time.Duration.between(previous.getTimestamp(), latest.getTimestamp()).getSeconds();
        
        // BUG T10: Hardcoded threshold without GPS accuracy
        if (distance < 5.0 && timeDiff > stationaryThresholdSeconds) {
            createAlert(Alert.builder()
                    .ride(latest.getRide())
                    .user(latest.getUser())
                    .device(latest.getDevice())
                    .type(AlertType.STATIONARY)
                    .message(String.format("User %s has been stationary for %d seconds", userId, timeDiff))
                    .isRead(false)
                    .build());
        }
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
        // BUG T12: Template string not properly formatted
        String message = String.format(
            "User %s has been stationary for %d seconds"); // BUG T12: Broken template
        alert.setMessage(message);
        alert.setCreatedAt(LocalDateTime.now());
        
        alertRepository.save(alert);
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
