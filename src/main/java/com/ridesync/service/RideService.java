package com.ridesync.service;

import com.ridesync.dto.RideRequestDto;
import com.ridesync.model.Ride;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface RideService {
    
    Ride createRide(String name, String description, UUID groupId, UUID userId);
    
    List<Ride> getRidesByUser(UUID userId);
    
    List<Ride> getRidesByGroup(UUID groupId);
    
    Optional<Ride> findById(UUID rideId);
    
    Ride updateRide(UUID rideId, RideRequestDto rideRequest);
    
    void deleteRide(UUID rideId);
    
    Ride startRide(UUID rideId);
    
    Ride endRide(UUID rideId);
    
    Ride pauseRide(UUID rideId);
    
    Ride resumeRide(UUID rideId);
}