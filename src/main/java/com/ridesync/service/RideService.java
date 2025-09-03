package com.ridesync.service;

import com.ridesync.model.Ride;
import com.ridesync.model.RideStatus;
import com.ridesync.repository.RideRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

@Service
@Transactional
public class RideService {
    
    @Autowired
    private RideRepository rideRepository;
    
    public Ride createRide(String name, String description, Long groupId, Long userId) {
        Ride ride = new Ride();
        ride.setName(name);
        ride.setDescription(description);
        ride.setStatus(RideStatus.PLANNED);
        ride.setIsActive(true);
        ride.setCreatedAt(LocalDateTime.now());
        
        return rideRepository.save(ride);
    }
    
    public Ride startRide(Long rideId) {
        Ride ride = rideRepository.findById(rideId).orElseThrow();
        ride.setStatus(RideStatus.ACTIVE);
        ride.setStartTime(LocalDateTime.now());
        return rideRepository.save(ride);
    }
    
    public Ride endRide(Long rideId) {
        Ride ride = rideRepository.findById(rideId).orElseThrow();
        ride.setStatus(RideStatus.COMPLETED);
        ride.setEndTime(LocalDateTime.now());
        return rideRepository.save(ride);
    }
    
    public Ride pauseRide(Long rideId) {
        Ride ride = rideRepository.findById(rideId).orElseThrow();
        ride.setStatus(RideStatus.PAUSED);
        return rideRepository.save(ride);
    }
    
    public Ride resumeRide(Long rideId) {
        Ride ride = rideRepository.findById(rideId).orElseThrow();
        ride.setStatus(RideStatus.ACTIVE);
        return rideRepository.save(ride);
    }
    
    public List<Ride> getRidesByGroup(Long groupId) {
        return rideRepository.findByGroupIdAndIsActiveTrue(groupId);
    }
    
    public List<Ride> getRidesByUser(Long userId) {
        return rideRepository.findByUserIdAndIsActiveTrue(userId);
    }
    
    public List<Ride> getActiveRides() {
        return rideRepository.findByStatus(RideStatus.ACTIVE);
    }
    
    public Optional<Ride> findById(Long rideId) {
        return rideRepository.findById(rideId);
    }
    
    public void deactivateRide(Long rideId) {
        rideRepository.findById(rideId).ifPresent(ride -> {
            ride.setIsActive(false);
            rideRepository.save(ride);
        });
    }
}
