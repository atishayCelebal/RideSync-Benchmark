package com.ridesync.consumer;

import com.ridesync.model.LocationUpdate;
import com.ridesync.model.Ride;
import com.ridesync.model.RideStatus;
import com.ridesync.service.AnomalyDetectionService;
import com.ridesync.service.LocationService;
import com.ridesync.service.RideService;
import lombok.RequiredArgsConstructor;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Component;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

@Component
@RequiredArgsConstructor
public class LocationUpdateConsumer {
    private static final Logger logger = LoggerFactory.getLogger(LocationUpdateConsumer.class);
    private final SimpMessagingTemplate messagingTemplate;
    private final LocationService locationService;
    private final RideService rideService;
    private final AnomalyDetectionService anomalyDetectionService;
    
    // BUG T05: Kafka consumer processes inactive sessions
    // BUG T06: Malformed GPS payload crashes consumer
    @KafkaListener(topics = "location-updates", groupId = "ridesync-group")
    public void handleLocationUpdate(LocationUpdate locationUpdate) {
        try {
            // T06 FIXED: Validate GPS payload before processing
            if (!isValidLocationUpdate(locationUpdate)) {
                logger.warn("Invalid location update received, skipping: {}", locationUpdate);
                return;
            }
            
            // Check if ride is active (T05 bug fix)
            if (!isRideActive(locationUpdate.getRide())) {
                logger.warn("Ride is not active, skipping location update: {}", locationUpdate.getRide().getId());
                return;
            }
            locationService.processLocationUpdate(locationUpdate);
            
            // BUG T15: Over-broadcasting to all clients
            // Broadcast to all connected WebSocket clients
            Map<String, Object> broadcastData = new HashMap<>();
            broadcastData.put("userId", locationUpdate.getUser().getId());
            broadcastData.put("rideId", locationUpdate.getRide().getId());
            broadcastData.put("latitude", locationUpdate.getLatitude());
            broadcastData.put("longitude", locationUpdate.getLongitude());
            broadcastData.put("deviceId", locationUpdate.getDevice() != null ? locationUpdate.getDevice().getId() : null); // BUG T08: Using deviceId
            broadcastData.put("timestamp", locationUpdate.getTimestamp());
            
            messagingTemplate.convertAndSend("/topic/location.updates", broadcastData);
            
            // Trigger anomaly detection
            anomalyDetectionService.detectAnomalies(locationUpdate.getRide().getId());
            
        } catch (Exception e) {
            // T06 FIXED: Log error but don't halt consumer thread
            logger.error("Error processing location update: {}", e.getMessage(), e);
            // T06 FIXED: Don't re-throw exception to prevent consumer thread from halting
            // Just log and continue processing other messages
        }
    }
    private boolean isValidLocationUpdate(LocationUpdate locationUpdate) {
        if (locationUpdate == null) {
            logger.warn("Location update is null");
            return false;
        }
        if (locationUpdate.getLatitude() == null) {
            logger.warn("Latitude is required");
            return false;
        }
        if (locationUpdate.getLongitude() == null) {
            logger.warn("Longitude is required");
            return false;
        }
        if (locationUpdate.getTimestamp() == null) {
            logger.warn("Timestamp is required");
            return false;
        }
        if (locationUpdate.getUser() == null) {
            logger.warn("User is required");
            return false;
        }
        if (locationUpdate.getRide() == null) {
            logger.warn("Ride is required");
            return false;
        }
        
        // Validate coordinate ranges
        if (locationUpdate.getLatitude() < -90.0 || locationUpdate.getLatitude() > 90.0) {
            logger.warn("Latitude must be between -90 and 90, got: {}", locationUpdate.getLatitude());
            return false;
        }
        if (locationUpdate.getLongitude() < -180.0 || locationUpdate.getLongitude() > 180.0) {
            logger.warn("Longitude must be between -180 and 180, got: {}", locationUpdate.getLongitude());
            return false;
        }
        
        // Validate timestamp (not too old, not in future)
        LocalDateTime now = LocalDateTime.now();
        LocalDateTime oneYearAgo = now.minusYears(1);
        LocalDateTime oneHourFromNow = now.plusHours(1);
        if (locationUpdate.getTimestamp().isBefore(oneYearAgo) || 
            locationUpdate.getTimestamp().isAfter(oneHourFromNow)) {
            logger.warn("Timestamp must be within reasonable range, got: {}", locationUpdate.getTimestamp());
            return false;
        }
        
        // Validate accuracy if present
        if (locationUpdate.getAccuracy() != null && locationUpdate.getAccuracy() < 0) {
            logger.warn("Accuracy must be non-negative, got: {}", locationUpdate.getAccuracy());
            return false;
        }
        
        return true;
    }
    private boolean isRideActive(Ride ride) {
        return ride != null && 
               (ride.getStatus() == RideStatus.ACTIVE || 
                ride.getStatus() == RideStatus.PLANNED);
    }
    
    
    // BUG T17: Crash on Kafka drop – no retry
    @KafkaListener(topics = "ride-events", groupId = "ridesync-group")
    public void handleRideEvent(Map<String, Object> rideEvent) {
        try {
            String eventType = rideEvent.get("eventType").toString();
            UUID rideId = UUID.fromString(rideEvent.get("rideId").toString());
            
            // Process ride events
            if ("RIDE_STARTED".equals(eventType)) {
                rideService.startRide(rideId);
            } else if ("RIDE_ENDED".equals(eventType)) {
                rideService.endRide(rideId);
            }
            
        } catch (Exception e) {
            // BUG T17: No retry/backoff mechanism
            System.err.println("Error processing ride event: " + e.getMessage());
            throw new RuntimeException("Failed to process ride event", e);
        }
    }
}
