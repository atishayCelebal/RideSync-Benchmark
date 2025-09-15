package com.ridesync.consumer;

import com.ridesync.dto.LocationUpdateKafkaDto;
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
import org.springframework.transaction.annotation.Transactional;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;
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
    @Transactional
    public void handleLocationUpdate(LocationUpdateKafkaDto kafkaDto) {
        logger.info("Kafka message received in consumer: {}", kafkaDto);
        try {
            // T06 FIXED: Validate GPS payload before processing
            if (!isValidLocationUpdate(kafkaDto)) {
                logger.warn("Invalid location update received, skipping: {}", kafkaDto);
                return;
            }
            
            // Check if ride is active (T05 bug fix) - we need to fetch the ride from DB
            Optional<Ride> rideOpt = rideService.findById(kafkaDto.getRideId());
            logger.info("Ride lookup result: {}", rideOpt.isPresent() ? "Found" : "Not found");
            if (rideOpt.isEmpty() || !isRideActive(rideOpt.get())) {
                logger.warn("Ride is not active or not found, skipping location update: {}", kafkaDto.getRideId());
                return;
            }
            logger.info("Ride validation passed, proceeding with WebSocket broadcast");
            
            // Create a LocationUpdate object for processing (if needed)
            LocationUpdate locationUpdate = new LocationUpdate();
            locationUpdate.setId(kafkaDto.getLocationUpdateId());
            locationUpdate.setLatitude(kafkaDto.getLatitude());
            locationUpdate.setLongitude(kafkaDto.getLongitude());
            locationUpdate.setAltitude(kafkaDto.getAltitude());
            locationUpdate.setSpeed(kafkaDto.getSpeed());
            locationUpdate.setHeading(kafkaDto.getHeading());
            locationUpdate.setAccuracy(kafkaDto.getAccuracy());
            locationUpdate.setTimestamp(kafkaDto.getTimestamp());
            // locationUpdate.setRide(rideOpt.get()); // Set the ride from the fetched data
            
            // locationService.processLocationUpdate(locationUpdate);
            
            // BUG T15: Over-broadcasting to all clients
            // Broadcast to all connected WebSocket clients
            Map<String, Object> broadcastData = new HashMap<>();
            broadcastData.put("userId", kafkaDto.getUserId());
            broadcastData.put("rideId", kafkaDto.getRideId());
            broadcastData.put("groupId", kafkaDto.getGroupId());
            broadcastData.put("latitude", kafkaDto.getLatitude());
            broadcastData.put("longitude", kafkaDto.getLongitude());
            broadcastData.put("deviceId", kafkaDto.getDeviceId());
            broadcastData.put("timestamp", kafkaDto.getTimestamp());
            
            logger.info("Broadcasting location update to WebSocket clients: {}", broadcastData);
            messagingTemplate.convertAndSend("/topic/location.updates", broadcastData);
            logger.info("WebSocket message sent successfully to topic: /topic/location.updates");
            // Trigger anomaly detection
            anomalyDetectionService.detectAnomalies(kafkaDto.getRideId());
            
        } catch (Exception e) {
            // T06 FIXED: Log error but don't halt consumer thread
            logger.error("Error processing location update: {}", e.getMessage(), e);
            // T06 FIXED: Don't re-throw exception to prevent consumer thread from halting
            // Just log and continue processing other messages
        }
    }
    private boolean isValidLocationUpdate(LocationUpdateKafkaDto kafkaDto) {
        if (kafkaDto == null) {
            logger.warn("Location update is null");
            return false;
        }
        if (kafkaDto.getLatitude() == null) {
            logger.warn("Latitude is required");
            return false;
        }
        if (kafkaDto.getLongitude() == null) {
            logger.warn("Longitude is required");
            return false;
        }
        if (kafkaDto.getTimestamp() == null) {
            logger.warn("Timestamp is required");
            return false;
        }
        if (kafkaDto.getUserId() == null) {
            logger.warn("User ID is required");
            return false;
        }
        if (kafkaDto.getRideId() == null) {
            logger.warn("Ride ID is required");
            return false;
        }
        
        // Validate coordinate ranges
        if (kafkaDto.getLatitude() < -90.0 || kafkaDto.getLatitude() > 90.0) {
            logger.warn("Latitude must be between -90 and 90, got: {}", kafkaDto.getLatitude());
            return false;
        }
        if (kafkaDto.getLongitude() < -180.0 || kafkaDto.getLongitude() > 180.0) {
            logger.warn("Longitude must be between -180 and 180, got: {}", kafkaDto.getLongitude());
            return false;
        }
        
        // Validate timestamp (not too old, not in future)
        LocalDateTime now = LocalDateTime.now();
        LocalDateTime oneYearAgo = now.minusYears(1);
        LocalDateTime oneHourFromNow = now.plusHours(1);
        if (kafkaDto.getTimestamp().isBefore(oneYearAgo) || 
            kafkaDto.getTimestamp().isAfter(oneHourFromNow)) {
            logger.warn("Timestamp must be within reasonable range, got: {}", kafkaDto.getTimestamp());
            return false;
        }
        
        // Validate accuracy if present
        if (kafkaDto.getAccuracy() != null && kafkaDto.getAccuracy() < 0) {
            logger.warn("Accuracy must be non-negative, got: {}", kafkaDto.getAccuracy());
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
