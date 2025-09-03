package com.ridesync.controller;

import com.ridesync.dto.LocationUpdateDto;
import com.ridesync.model.LocationUpdate;
import com.ridesync.service.LocationService;
import jakarta.validation.Valid;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/location")
@CrossOrigin(origins = "*")
public class LocationController {
    
    @Autowired
    private LocationService locationService;
    
    // BUG T03: Insecure Location API – No JWT/session validation
    @PostMapping("/update")
    public ResponseEntity<?> updateLocation(@Valid @RequestBody LocationUpdateDto locationDto) {
        try {
            // BUG T03: No authentication/authorization check
            // BUG T03: No validation that userId in token matches payload
            LocationUpdate locationUpdate = locationService.saveLocationUpdate(locationDto);
            
            Map<String, Object> response = new HashMap<>();
            response.put("message", "Location updated successfully");
            response.put("locationId", locationUpdate.getId());
            
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            Map<String, String> error = new HashMap<>();
            error.put("error", "Failed to update location: " + e.getMessage());
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(error);
        }
    }
    
    @GetMapping("/ride/{rideId}")
    public ResponseEntity<?> getLocationUpdatesForRide(@PathVariable Long rideId) {
        try {
            List<LocationUpdate> updates = locationService.getLocationUpdatesForRide(rideId);
            return ResponseEntity.ok(updates);
        } catch (Exception e) {
            Map<String, String> error = new HashMap<>();
            error.put("error", "Failed to fetch location updates: " + e.getMessage());
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(error);
        }
    }
    
    @GetMapping("/user/{userId}")
    public ResponseEntity<?> getLocationUpdatesForUser(@PathVariable Long userId) {
        try {
            List<LocationUpdate> updates = locationService.getLocationUpdatesForUser(userId);
            return ResponseEntity.ok(updates);
        } catch (Exception e) {
            Map<String, String> error = new HashMap<>();
            error.put("error", "Failed to fetch location updates: " + e.getMessage());
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(error);
        }
    }
    
    // BUG T13: Location data leakage – unrestricted API query
    @GetMapping("/active")
    public ResponseEntity<?> getAllActiveLocationUpdates() {
        try {
            // BUG T13: Returns all active locations without group filtering
            List<LocationUpdate> updates = locationService.getAllActiveLocationUpdates();
            return ResponseEntity.ok(updates);
        } catch (Exception e) {
            Map<String, String> error = new HashMap<>();
            error.put("error", "Failed to fetch active location updates: " + e.getMessage());
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(error);
        }
    }
    
    @GetMapping("/nearby")
    public ResponseEntity<?> getNearbyLocationUpdates(@RequestParam Double latitude,
                                                    @RequestParam Double longitude,
                                                    @RequestParam(defaultValue = "1000") Double radius) {
        try {
            List<LocationUpdate> updates = locationService.getNearbyLocationUpdates(latitude, longitude, radius);
            return ResponseEntity.ok(updates);
        } catch (Exception e) {
            Map<String, String> error = new HashMap<>();
            error.put("error", "Failed to fetch nearby location updates: " + e.getMessage());
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(error);
        }
    }
}
