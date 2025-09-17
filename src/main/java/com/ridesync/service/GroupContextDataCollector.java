package com.ridesync.service;

import com.ridesync.model.LocationUpdate;
import com.ridesync.model.Ride;
import com.ridesync.model.User;
import com.ridesync.repository.LocationUpdateRepository;
import com.ridesync.repository.RideRepository;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.stream.Collectors;

/**
 * Service for collecting and formatting group context data for LLM analysis
 */
@Service
@RequiredArgsConstructor
public class GroupContextDataCollector {
    
    private static final Logger logger = LoggerFactory.getLogger(GroupContextDataCollector.class);
    
    private final LocationUpdateRepository locationUpdateRepository;
    private final RideRepository rideRepository;
    
    // Number of recent locations to include in history
    private static final int HISTORY_LIMIT = 5;
    
    /**
     * Collect comprehensive group context data for LLM analysis
     * @param rideId The ride ID
     * @param userId The current user ID whose location triggered the analysis
     * @return Map containing formatted data for LLM
     */
    public Map<String, Object> collectGroupContextData(UUID rideId, UUID userId) {
        logger.info("Collecting group context data for ride: {}, user: {}", rideId, userId);
        
        try {
            // Get ride information
            Ride ride = rideRepository.findById(rideId)
                    .orElseThrow(() -> new RuntimeException("Ride not found: " + rideId));
            
            // Get current user's recent history (last 5 locations)
            List<LocationUpdate> userHistory = getUserRecentHistory(userId, HISTORY_LIMIT);
            
            // Get all active group members
            List<User> activeMembers = getActiveGroupMembers(rideId);
            
            // Get last location for each group member
            List<Map<String, Object>> groupMemberLocations = getGroupMemberLastLocations(activeMembers);
            
            // Build context data
            Map<String, Object> contextData = new HashMap<>();
            
            // Current user data
            Map<String, Object> currentUser = new HashMap<>();
            currentUser.put("userId", userId);
            
            if (!userHistory.isEmpty()) {
                LocationUpdate currentLocation = userHistory.get(0); // Most recent
                currentUser.put("currentLocation", formatLocationUpdate(currentLocation));
                currentUser.put("recentHistory", userHistory.stream()
                        .map(this::formatLocationUpdate)
                        .collect(Collectors.toList()));
            }
            
            contextData.put("currentUser", currentUser);
            contextData.put("groupMembers", groupMemberLocations);
            
            // Ride context
            Map<String, Object> rideContext = new HashMap<>();
            rideContext.put("rideId", rideId);
            rideContext.put("rideName", ride.getName());
            rideContext.put("startTime", ride.getStartTime());
            rideContext.put("groupSize", activeMembers.size());
            rideContext.put("expectedRoute", ride.getDescription());
            rideContext.put("rideStatus", ride.getStatus());
            
            contextData.put("rideContext", rideContext);
            
            // Analysis metadata
            Map<String, Object> metadata = new HashMap<>();
            metadata.put("analysisTimestamp", LocalDateTime.now().format(DateTimeFormatter.ISO_LOCAL_DATE_TIME));
            metadata.put("dataCollectionTime", System.currentTimeMillis());
            metadata.put("historyLimit", HISTORY_LIMIT);
            
            contextData.put("metadata", metadata);
            
            logger.info("Successfully collected context data for {} group members", activeMembers.size());
            return contextData;
            
        } catch (Exception e) {
            logger.error("Error collecting group context data: {}", e.getMessage(), e);
            throw new RuntimeException("Failed to collect group context data", e);
        }
    }
    
    /**
     * Get recent location history for a user
     * @param userId The user ID
     * @param limit Number of recent locations to retrieve
     * @return List of recent location updates
     */
    private List<LocationUpdate> getUserRecentHistory(UUID userId, int limit) {
        return locationUpdateRepository.findByUserIdOrderByTimestampDesc(userId)
                .stream()
                .limit(limit)
                .collect(Collectors.toList());
    }
    
    /**
     * Get all active group members for a ride
     * @param rideId The ride ID
     * @return List of active users
     */
    private List<User> getActiveGroupMembers(UUID rideId) {
        // Get all location updates for this ride, then extract unique users
        List<LocationUpdate> rideUpdates = locationUpdateRepository.findByRideIdOrderByTimestampDesc(rideId);
        
        return rideUpdates.stream()
                .map(LocationUpdate::getUser)
                .distinct()
                .collect(Collectors.toList());
    }
    
    /**
     * Get last location for each group member
     * @param activeMembers List of active group members
     * @return List of member location data
     */
    private List<Map<String, Object>> getGroupMemberLastLocations(List<User> activeMembers) {
        return activeMembers.stream()
                .map(member -> {
                    Map<String, Object> memberData = new HashMap<>();
                    memberData.put("userId", member.getId());
                    memberData.put("userName", member.getUsername());
                    
                    // Get last location for this member
                    List<LocationUpdate> memberLocations = locationUpdateRepository
                            .findByUserIdOrderByTimestampDesc(member.getId());
                    
                    if (!memberLocations.isEmpty()) {
                        LocationUpdate lastLocation = memberLocations.get(0);
                        memberData.put("currentLocation", formatLocationUpdate(lastLocation));
                    } else {
                        memberData.put("currentLocation", null);
                    }
                    
                    return memberData;
                })
                .collect(Collectors.toList());
    }
    
    /**
     * Format a LocationUpdate object for LLM consumption
     * @param locationUpdate The location update to format
     * @return Formatted location data
     */
    private Map<String, Object> formatLocationUpdate(LocationUpdate locationUpdate) {
        Map<String, Object> formatted = new HashMap<>();
        formatted.put("latitude", locationUpdate.getLatitude());
        formatted.put("longitude", locationUpdate.getLongitude());
        formatted.put("speed", locationUpdate.getSpeed());
        formatted.put("heading", locationUpdate.getHeading());
        formatted.put("accuracy", locationUpdate.getAccuracy());
        formatted.put("altitude", locationUpdate.getAltitude());
        formatted.put("timestamp", locationUpdate.getTimestamp().format(DateTimeFormatter.ISO_LOCAL_DATE_TIME));
        return formatted;
    }
}
