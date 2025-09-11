package com.ridesync.service;

import com.ridesync.dto.LocationUpdateDto;
import com.ridesync.model.LocationUpdate;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

public interface LocationService {
    
    LocationUpdate saveLocationUpdate(LocationUpdateDto locationDto);
    
    List<LocationUpdate> getLocationUpdatesForRide(UUID rideId);
    
    List<LocationUpdate> getLocationUpdatesForUser(UUID userId);
    
    List<LocationUpdate> getGroupLocationUpdatesForRide(UUID groupId, UUID rideId);
    
    List<LocationUpdate> getCurrentGroupLocations(UUID groupId);
    
    List<LocationUpdate> getAllActiveLocationUpdates();
    
    // FIXED T13: Group-filtered method - only returns location updates from user's groups
    List<LocationUpdate> getActiveLocationUpdatesByUserGroups(UUID userId);
    
    List<LocationUpdate> getNearbyLocationUpdates(Double latitude, Double longitude, Double radius);
    
    List<LocationUpdate> getLocationUpdatesByDeviceId(UUID rideId, UUID deviceId);
    
    List<LocationUpdate> getRecentLocationUpdates(UUID rideId, LocalDateTime since);
    
    void processLocationUpdate(LocationUpdate locationUpdate);
}