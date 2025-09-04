package com.ridesync.service;

import com.ridesync.dto.RideRequestDto;
import com.ridesync.exception.ResourceNotFoundException;
import com.ridesync.model.Group;
import com.ridesync.model.Ride;
import com.ridesync.model.RideStatus;
import com.ridesync.model.User;
import com.ridesync.repository.GroupRepository;
import com.ridesync.repository.RideRepository;
import com.ridesync.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Service
@Transactional
@RequiredArgsConstructor
public class RideService {
    
    private final RideRepository rideRepository;
    private final GroupRepository groupRepository;
    private final UserRepository userRepository;
    
    public Ride createRide(String name, String description, UUID groupId, UUID userId) {
        Group group = groupRepository.findById(groupId)
                .orElseThrow(() -> new ResourceNotFoundException("Group", "id", groupId));
        
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new ResourceNotFoundException("User", "id", userId));
        
        Ride ride = Ride.builder()
                .name(name)
                .description(description)
                .status(RideStatus.PLANNED)
                .isActive(true)
                .group(group)
                .createdBy(user)
                .build();
        
        return rideRepository.save(ride);
    }
    
    public List<Ride> getRidesByUser(UUID userId) {
        return rideRepository.findByUserIdAndIsActiveTrue(userId);
    }
    
    public List<Ride> getRidesByGroup(UUID groupId) {
        return rideRepository.findByGroupIdAndIsActiveTrue(groupId);
    }
    
    public Optional<Ride> findById(UUID rideId) {
        return rideRepository.findById(rideId);
    }
    
    public Ride updateRide(UUID rideId, RideRequestDto rideRequest) {
        Ride ride = rideRepository.findById(rideId)
                .orElseThrow(() -> new ResourceNotFoundException("Ride", "id", rideId));
        
        ride.setName(rideRequest.getName());
        ride.setDescription(rideRequest.getDescription());
        ride.setUpdatedAt(LocalDateTime.now());
        
        return rideRepository.save(ride);
    }
    
    public void deleteRide(UUID rideId) {
        Ride ride = rideRepository.findById(rideId)
                .orElseThrow(() -> new ResourceNotFoundException("Ride", "id", rideId));
        
        ride.setIsActive(false);
        ride.setUpdatedAt(LocalDateTime.now());
        rideRepository.save(ride);
    }
    
    public Ride startRide(UUID rideId) {
        Ride ride = rideRepository.findById(rideId)
                .orElseThrow(() -> new ResourceNotFoundException("Ride", "id", rideId));
        
        ride.setStatus(RideStatus.ACTIVE);
        ride.setStartTime(LocalDateTime.now());
        ride.setUpdatedAt(LocalDateTime.now());
        
        return rideRepository.save(ride);
    }
    
    public Ride endRide(UUID rideId) {
        Ride ride = rideRepository.findById(rideId)
                .orElseThrow(() -> new ResourceNotFoundException("Ride", "id", rideId));
        
        ride.setStatus(RideStatus.COMPLETED);
        ride.setEndTime(LocalDateTime.now());
        ride.setUpdatedAt(LocalDateTime.now());
        
        return rideRepository.save(ride);
    }
    
    public Ride pauseRide(UUID rideId) {
        Ride ride = rideRepository.findById(rideId)
                .orElseThrow(() -> new ResourceNotFoundException("Ride", "id", rideId));
        
        ride.setStatus(RideStatus.PAUSED);
        ride.setUpdatedAt(LocalDateTime.now());
        
        return rideRepository.save(ride);
    }
    
    public Ride resumeRide(UUID rideId) {
        Ride ride = rideRepository.findById(rideId)
                .orElseThrow(() -> new ResourceNotFoundException("Ride", "id", rideId));
        
        ride.setStatus(RideStatus.ACTIVE);
        ride.setUpdatedAt(LocalDateTime.now());
        
        return rideRepository.save(ride);
    }
}