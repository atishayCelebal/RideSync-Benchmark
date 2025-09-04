package com.ridesync.service;

import com.ridesync.model.Alert;
import com.ridesync.model.AlertType;
import com.ridesync.model.LocationUpdate;
import com.ridesync.model.Ride;
import com.ridesync.repository.AlertRepository;
import com.ridesync.repository.LocationUpdateRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;

@Service
public class AnomalyDetectionService {
    
    @Autowired
    private AlertRepository alertRepository;
    
    @Autowired
    private LocationUpdateRepository locationUpdateRepository;
    
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
    private void detectStationaryAnomaly(UUID userId, List<LocationUpdate> updates) {
        if (updates.size() < 2) return;
        
        LocationUpdate latest = updates.get(0);
        LocationUpdate previous = updates.get(1);
        
        // BUG T10: Simple distance check without speed/distance thresholds
        double distance = calculateDistance(
                latest.getLatitude(), latest.getLongitude(),
                previous.getLatitude(), previous.getLongitude()
        );
        
        // BUG T10: No filtering for GPS jitter
        if (distance < 10) { // 10 meters threshold
            // BUG T19: False alerts in stop-and-go traffic - no timer reset
            createAlert(latest.getRide(), latest.getUser(), AlertType.STATIONARY, 
                       "User %s has been stationary for %d seconds"); // BUG T12: Broken template
        }
    }
    
    // BUG T11: Direction drift miscalculation
    private void detectDirectionDrift(UUID userId, List<LocationUpdate> updates) {
        if (updates.size() < 3) return;
        
        LocationUpdate latest = updates.get(0);
        
        // BUG T11: Wrong base heading calculation
        double userHeading = latest.getHeading();
        double groupHeading = calculateGroupHeading(updates); // This method has bugs
        
        double drift = Math.abs(userHeading - groupHeading);
        
        if (drift > 45) { // 45 degrees threshold
            createAlert(latest.getRide(), latest.getUser(), AlertType.DIRECTION_DRIFT,
                       "User %s is drifting from group direction by %.1f degrees"); // BUG T12: Broken template
        }
    }
    
    private double calculateGroupHeading(List<LocationUpdate> updates) {
        // BUG T11: Simple average instead of median calculation
        return updates.stream()
                .filter(u -> u.getHeading() != null)
                .mapToDouble(LocationUpdate::getHeading)
                .average()
                .orElse(0.0);
    }
    
    private double calculateDistance(double lat1, double lon1, double lat2, double lon2) {
        final int R = 6371; // Radius of the earth in km
        double latDistance = Math.toRadians(lat2 - lat1);
        double lonDistance = Math.toRadians(lon2 - lon1);
        double a = Math.sin(latDistance / 2) * Math.sin(latDistance / 2)
                + Math.cos(Math.toRadians(lat1)) * Math.cos(Math.toRadians(lat2))
                * Math.sin(lonDistance / 2) * Math.sin(lonDistance / 2);
        double c = 2 * Math.atan2(Math.sqrt(a), Math.sqrt(1 - a));
        return R * c * 1000; // Convert to meters
    }
    
    // BUG T12: Alert message template broken
    private void createAlert(Ride ride, com.ridesync.model.User user, AlertType type, String template) {
        // BUG T12: String.format with missing placeholders
        String message = String.format(template, user.getUsername(), 0, 0.0);
        
        Alert alert = new Alert();
        alert.setRide(ride);
        alert.setUser(user);
        alert.setType(type);
        alert.setMessage(message);
        alert.setSeverity("WARNING");
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
}
