package com.ridesync.consumer;

import com.ridesync.dto.LocationUpdateDto;
import com.ridesync.model.LocationUpdate;
import com.ridesync.model.Ride;
import com.ridesync.model.RideStatus;
import com.ridesync.service.AnomalyDetectionService;
import com.ridesync.service.LocationService;
import com.ridesync.service.RideService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Component;

import java.util.HashMap;
import java.util.Map;

@Component
public class LocationUpdateConsumer {
    
    @Autowired
    private SimpMessagingTemplate messagingTemplate;
    
    @Autowired
    private LocationService locationService;
    
    @Autowired
    private RideService rideService;
    
    @Autowired
    private AnomalyDetectionService anomalyDetectionService;
    
    // BUG T05: Kafka consumer processes inactive sessions
    // BUG T06: Malformed GPS payload crashes consumer
    @KafkaListener(topics = "location-updates", groupId = "ridesync-group")
    public void handleLocationUpdate(LocationUpdate locationUpdate) {
        try {
            // BUG T05: No check if ride is active
            // BUG T06: No validation of GPS payload - will crash on malformed data
            
            // Process location update regardless of ride status
            locationService.processLocationUpdate(locationUpdate);
            
            // BUG T15: Over-broadcasting to all clients
            // Broadcast to all connected WebSocket clients
            Map<String, Object> broadcastData = new HashMap<>();
            broadcastData.put("userId", locationUpdate.getUser().getId());
            broadcastData.put("rideId", locationUpdate.getRide().getId());
            broadcastData.put("latitude", locationUpdate.getLatitude());
            broadcastData.put("longitude", locationUpdate.getLongitude());
            broadcastData.put("deviceId", locationUpdate.getDeviceId()); // BUG T08: Using deviceId
            broadcastData.put("timestamp", locationUpdate.getTimestamp());
            
            messagingTemplate.convertAndSend("/topic/location.updates", broadcastData);
            
            // Trigger anomaly detection
            anomalyDetectionService.detectAnomalies(locationUpdate.getRide().getId());
            
        } catch (Exception e) {
            // BUG T06: Exception handling - consumer thread will halt
            System.err.println("Error processing location update: " + e.getMessage());
            e.printStackTrace();
            // BUG T17: No retry mechanism on Kafka failure
            throw new RuntimeException("Failed to process location update", e);
        }
    }
    
    // BUG T17: Crash on Kafka drop – no retry
    @KafkaListener(topics = "ride-events", groupId = "ridesync-group")
    public void handleRideEvent(Map<String, Object> rideEvent) {
        try {
            String eventType = rideEvent.get("eventType").toString();
            Long rideId = Long.valueOf(rideEvent.get("rideId").toString());
            
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
