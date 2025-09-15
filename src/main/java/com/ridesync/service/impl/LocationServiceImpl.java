package com.ridesync.service.impl;

import com.ridesync.dto.LocationUpdateDto;
import com.ridesync.dto.LocationUpdateKafkaDto;
import com.ridesync.model.LocationUpdate;
import com.ridesync.model.Ride;
import com.ridesync.model.User;
import com.ridesync.repository.LocationUpdateRepository;
import com.ridesync.repository.RideRepository;
import com.ridesync.repository.UserRepository;
import com.ridesync.service.LocationService;
import lombok.RequiredArgsConstructor;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

@Service
@Transactional
@RequiredArgsConstructor
public class LocationServiceImpl implements LocationService {
    
    private final LocationUpdateRepository locationUpdateRepository;
    private final RideRepository rideRepository;
    private final UserRepository userRepository;
    private final SimpMessagingTemplate messagingTemplate;
    private final KafkaTemplate<String, LocationUpdateKafkaDto> kafkaTemplate;
    
    // BUG T03: No JWT/session validation
    public LocationUpdate saveLocationUpdate(LocationUpdateDto locationDto) {
        // BUG T03: No authentication/authorization check
        // BUG T03: No validation that userId in token matches payload
        
        User user = userRepository.findById(locationDto.getUserId()).orElseThrow();
        Ride ride = rideRepository.findById(locationDto.getRideId()).orElseThrow();
        
        LocationUpdate locationUpdate = new LocationUpdate();
        locationUpdate.setRide(ride);
        locationUpdate.setUser(user);
        locationUpdate.setLatitude(locationDto.getLatitude());
        locationUpdate.setLongitude(locationDto.getLongitude());
        locationUpdate.setAltitude(locationDto.getAltitude());
        locationUpdate.setSpeed(locationDto.getSpeed());
        locationUpdate.setHeading(locationDto.getHeading());
        locationUpdate.setAccuracy(locationDto.getAccuracy());
        // TODO: Need to get Device entity from deviceId
        // locationUpdate.setDevice(device);
        locationUpdate.setTimestamp(locationDto.getTimestamp() != null ? locationDto.getTimestamp() : LocalDateTime.now());
        
        LocationUpdate savedUpdate = locationUpdateRepository.save(locationUpdate);
        
        // Create Kafka DTO with essential data only
        LocationUpdateKafkaDto kafkaDto = LocationUpdateKafkaDto.builder()
                .locationUpdateId(savedUpdate.getId())
                .userId(savedUpdate.getUser().getId())
                .rideId(savedUpdate.getRide().getId())
                .groupId(savedUpdate.getRide().getGroup().getId())
                .latitude(savedUpdate.getLatitude())
                .longitude(savedUpdate.getLongitude())
                .altitude(savedUpdate.getAltitude())
                .speed(savedUpdate.getSpeed())
                .heading(savedUpdate.getHeading())
                .accuracy(savedUpdate.getAccuracy())
                .timestamp(savedUpdate.getTimestamp())
                .deviceId(locationDto.getDeviceId())
                .build();
        
        // Publish to Kafka for real-time processing
        messagingTemplate.convertAndSend("/topic/location.updates", "broadcastData");
        kafkaTemplate.send("location-updates", kafkaDto);
        
        return savedUpdate;
    }
    
    public List<LocationUpdate> getLocationUpdatesForRide(UUID rideId) {
        return locationUpdateRepository.findByRideIdOrderByTimestampDesc(rideId);
    }
    
    public List<LocationUpdate> getLocationUpdatesForUser(UUID userId) {
        return locationUpdateRepository.findByUserIdOrderByTimestampDesc(userId);
    }
    
    // Get locations of all group members during a specific ride
    public List<LocationUpdate> getGroupLocationUpdatesForRide(UUID groupId, UUID rideId) {
        return locationUpdateRepository.findByGroupIdAndRideIdOrderByTimestampDesc(groupId, rideId);
    }
    
    // Get current locations of all active group members
    public List<LocationUpdate> getCurrentGroupLocations(UUID groupId) {
        return locationUpdateRepository.findCurrentGroupLocations(groupId);
    }
    
    // BUG T13: Location data leakage - unrestricted API query
    public List<LocationUpdate> getAllActiveLocationUpdates() {
        // BUG T13: Returns all active locations without group filtering
        return locationUpdateRepository.findAllActiveLocationUpdates();
    }
    
    // FIXED T13: Group-filtered method - only returns location updates from user's groups
    public List<LocationUpdate> getActiveLocationUpdatesByUserGroups(UUID userId) {
        // FIXED T13: Only returns location updates from groups where user is an active member
        return locationUpdateRepository.findActiveLocationUpdatesByUserGroups(userId);
    }
    
    // BUG T16: Inefficient nearby query - no bounding box filter
    public List<LocationUpdate> getNearbyLocationUpdates(Double latitude, Double longitude, Double radius) {
        // BUG T16: No spatial index optimization, scans all records
        return locationUpdateRepository.findNearbyLocationUpdates(latitude, longitude, radius);
    }
    
    public List<LocationUpdate> getLocationUpdatesByDeviceId(UUID rideId, UUID deviceId) {
        return locationUpdateRepository.findByRideIdAndDeviceId(rideId, deviceId);
    }
    
    public List<LocationUpdate> getRecentLocationUpdates(UUID rideId, LocalDateTime since) {
        return locationUpdateRepository.findByRideIdAndTimestampAfter(rideId, since);
    }
    
    // BUG T09: Processes updates when ride inactive
    public void processLocationUpdate(LocationUpdate locationUpdate) {
        // BUG T09: No check if ride is active
        Ride ride = locationUpdate.getRide();
        
        // Process location update regardless of ride status
        // This should check if ride.isActive and ride.status == ACTIVE
        
        // Simulate some processing
        System.out.println("Processing location update for ride: " + ride.getId());
    }
}
