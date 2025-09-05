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
    
    List<LocationUpdate> getNearbyLocationUpdates(Double latitude, Double longitude, Double radius);
    
    List<LocationUpdate> getLocationUpdatesByDeviceId(UUID rideId, UUID deviceId);
    
    List<LocationUpdate> getRecentLocationUpdates(UUID rideId, LocalDateTime since);
    
    void processLocationUpdate(LocationUpdate locationUpdate);
}