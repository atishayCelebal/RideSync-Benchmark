package com.ridesync.controller;

import com.ridesync.model.LocationUpdate;
import com.ridesync.service.GroupService;
import com.ridesync.service.LocationService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.messaging.handler.annotation.MessageMapping;
import org.springframework.messaging.handler.annotation.Payload;
import org.springframework.messaging.simp.SimpMessageHeaderAccessor;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Controller;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

@Controller
public class WebSocketController {
    
    @Autowired
    private SimpMessagingTemplate messagingTemplate;
    
    @Autowired
    private LocationService locationService;
    
    @Autowired
    private GroupService groupService;
    
    // BUG T04: Multiple active sessions per user – duplicate map markers
    // BUG T07: WebSocket allows join without group check
    private final Map<String, String> activeSessions = new ConcurrentHashMap<>();
    
    @MessageMapping("/location.update")
    public void handleLocationUpdate(@Payload Map<String, Object> locationData,
                                   SimpMessageHeaderAccessor headerAccessor) {
        try {
            String sessionId = headerAccessor.getSessionId();
            Long userId = Long.valueOf(locationData.get("userId").toString());
            Long rideId = Long.valueOf(locationData.get("rideId").toString());
            
            // BUG T04: No check for multiple active sessions per user
            // BUG T07: No group membership validation
            
            // BUG T15: Over-broadcasting to all clients
            // Broadcast to all connected clients instead of targeted group
            messagingTemplate.convertAndSend("/topic/location.updates", locationData);
            
            // Store session info
            activeSessions.put(sessionId, userId.toString());
            
        } catch (Exception e) {
            System.err.println("Error handling location update: " + e.getMessage());
        }
    }
    
    @MessageMapping("/ride.join")
    public void handleRideJoin(@Payload Map<String, Object> joinData,
                              SimpMessageHeaderAccessor headerAccessor) {
        try {
            String sessionId = headerAccessor.getSessionId();
            Long userId = Long.valueOf(joinData.get("userId").toString());
            Long rideId = Long.valueOf(joinData.get("rideId").toString());
            
            // BUG T07: No group membership check before allowing join
            // Should validate that user is member of the group that owns the ride
            
            // Subscribe to ride-specific topic
            messagingTemplate.convertAndSend("/topic/ride." + rideId, 
                Map.of("message", "User joined ride", "userId", userId));
            
            activeSessions.put(sessionId, userId.toString());
            
        } catch (Exception e) {
            System.err.println("Error handling ride join: " + e.getMessage());
        }
    }
    
    @MessageMapping("/ride.leave")
    public void handleRideLeave(@Payload Map<String, Object> leaveData,
                               SimpMessageHeaderAccessor headerAccessor) {
        try {
            String sessionId = headerAccessor.getSessionId();
            Long userId = Long.valueOf(leaveData.get("userId").toString());
            Long rideId = Long.valueOf(leaveData.get("rideId").toString());
            
            // Remove from active sessions
            activeSessions.remove(sessionId);
            
            // Notify others
            messagingTemplate.convertAndSend("/topic/ride." + rideId, 
                Map.of("message", "User left ride", "userId", userId));
            
        } catch (Exception e) {
            System.err.println("Error handling ride leave: " + e.getMessage());
        }
    }
    
    // BUG T14: Latency due to fallback polling
    @MessageMapping("/ping")
    public void handlePing(@Payload Map<String, Object> pingData,
                          SimpMessageHeaderAccessor headerAccessor) {
        try {
            String sessionId = headerAccessor.getSessionId();
            
            // BUG T14: Simple response without connection optimization
            messagingTemplate.convertAndSendToUser(sessionId, "/queue/pong", 
                Map.of("timestamp", System.currentTimeMillis()));
            
        } catch (Exception e) {
            System.err.println("Error handling ping: " + e.getMessage());
        }
    }
}
