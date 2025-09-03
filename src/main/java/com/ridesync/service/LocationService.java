package com.ridesync.service;

import com.ridesync.dto.LocationUpdateDto;
import com.ridesync.model.LocationUpdate;
import com.ridesync.model.Ride;
import com.ridesync.model.RideStatus;
import com.ridesync.model.User;
import com.ridesync.repository.LocationUpdateRepository;
import com.ridesync.repository.RideRepository;
import com.ridesync.repository.UserRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

@Service
@Transactional
public class LocationService {
    
    @Autowired
    private LocationUpdateRepository locationUpdateRepository;
    
    @Autowired
    private RideRepository rideRepository;
    
    @Autowired
    private UserRepository userRepository;
    
    @Autowired
    private KafkaTemplate<String, Object> kafkaTemplate;
    
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
        locationUpdate.setDeviceId(locationDto.getDeviceId());
        locationUpdate.setTimestamp(locationDto.getTimestamp() != null ? locationDto.getTimestamp() : LocalDateTime.now());
        
        LocationUpdate savedUpdate = locationUpdateRepository.save(locationUpdate);
        
        // Publish to Kafka for real-time processing
        kafkaTemplate.send("location-updates", savedUpdate);
        
        return savedUpdate;
    }
    
    public List<LocationUpdate> getLocationUpdatesForRide(Long rideId) {
        return locationUpdateRepository.findByRideIdOrderByTimestampDesc(rideId);
    }
    
    public List<LocationUpdate> getLocationUpdatesForUser(Long userId) {
        return locationUpdateRepository.findByUserIdOrderByTimestampDesc(userId);
    }
    
    // BUG T13: Location data leakage - unrestricted API query
    public List<LocationUpdate> getAllActiveLocationUpdates() {
        // BUG T13: Returns all active locations without group filtering
        return locationUpdateRepository.findAllActiveLocationUpdates();
    }
    
    // BUG T16: Inefficient nearby query - no bounding box filter
    public List<LocationUpdate> getNearbyLocationUpdates(Double latitude, Double longitude, Double radius) {
        // BUG T16: No spatial index optimization, scans all records
        return locationUpdateRepository.findNearbyLocationUpdates(latitude, longitude, radius);
    }
    
    public List<LocationUpdate> getLocationUpdatesByDeviceId(Long rideId, String deviceId) {
        return locationUpdateRepository.findByRideIdAndDeviceId(rideId, deviceId);
    }
    
    public List<LocationUpdate> getRecentLocationUpdates(Long rideId, LocalDateTime since) {
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
