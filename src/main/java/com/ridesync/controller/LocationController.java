package com.ridesync.controller;

import com.ridesync.dto.ApiResponse;
import com.ridesync.dto.LocationUpdateDto;
import com.ridesync.dto.LocationUpdateResponseDto;
import com.ridesync.mapper.LocationMapper;
import com.ridesync.model.LocationUpdate;
import com.ridesync.service.LocationService;
import jakarta.validation.Valid;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/location")
@CrossOrigin(origins = "*")
public class LocationController {
    
    @Autowired
    private LocationService locationService;
    
    @Autowired
    private LocationMapper locationMapper;
    
    // BUG T03: Insecure Location API – No JWT/session validation
    @PostMapping("/update")
    public ResponseEntity<ApiResponse<LocationUpdateResponseDto>> updateLocation(@Valid @RequestBody LocationUpdateDto locationDto) {
        // BUG T03: No authentication/authorization check
        // BUG T03: No validation that userId in token matches payload
        LocationUpdate locationUpdate = locationService.saveLocationUpdate(locationDto);
        return ResponseEntity.ok(ApiResponse.success("Location updated successfully", 
                locationMapper.toLocationUpdateResponseDto(locationUpdate)));
    }
    
    @GetMapping("/ride/{rideId}")
    public ResponseEntity<ApiResponse<List<LocationUpdateResponseDto>>> getLocationUpdatesForRide(@PathVariable Long rideId) {
        List<LocationUpdate> updates = locationService.getLocationUpdatesForRide(rideId);
        return ResponseEntity.ok(ApiResponse.success("Location updates retrieved successfully", 
                locationMapper.toLocationUpdateResponseDtoList(updates)));
    }
    
    @GetMapping("/user/{userId}")
    public ResponseEntity<ApiResponse<List<LocationUpdateResponseDto>>> getLocationUpdatesForUser(@PathVariable Long userId) {
        List<LocationUpdate> updates = locationService.getLocationUpdatesForUser(userId);
        return ResponseEntity.ok(ApiResponse.success("User location updates retrieved successfully", 
                locationMapper.toLocationUpdateResponseDtoList(updates)));
    }
    
    // BUG T13: Location data leakage – unrestricted API query
    @GetMapping("/active")
    public ResponseEntity<ApiResponse<List<LocationUpdateResponseDto>>> getAllActiveLocationUpdates() {
        // BUG T13: Returns all active locations without group filtering
        List<LocationUpdate> updates = locationService.getAllActiveLocationUpdates();
        return ResponseEntity.ok(ApiResponse.success("Active location updates retrieved successfully", 
                locationMapper.toLocationUpdateResponseDtoList(updates)));
    }
    
    @GetMapping("/nearby")
    public ResponseEntity<ApiResponse<List<LocationUpdateResponseDto>>> getNearbyLocationUpdates(@RequestParam Double latitude,
                                                    @RequestParam Double longitude,
                                                    @RequestParam(defaultValue = "1000") Double radius) {
        List<LocationUpdate> updates = locationService.getNearbyLocationUpdates(latitude, longitude, radius);
        return ResponseEntity.ok(ApiResponse.success("Nearby location updates retrieved successfully", 
                locationMapper.toLocationUpdateResponseDtoList(updates)));
    }
}
