package com.ridesync.controller;

import com.ridesync.dto.ApiResponse;
import com.ridesync.dto.LocationUpdateDto;
import com.ridesync.dto.LocationUpdateResponseDto;
import com.ridesync.mapper.LocationMapper;
import com.ridesync.model.LocationUpdate;
import com.ridesync.model.User;
import com.ridesync.service.LocationService;
import com.ridesync.service.UserService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/v1/location")
@RequiredArgsConstructor
public class LocationController {
    
    private final LocationService locationService;
    private final LocationMapper locationMapper;
    private final UserService userService;
    
    // FIXED T03: Location API now requires authentication and user identity validation
    @PostMapping("/update")
    public ResponseEntity<ApiResponse<LocationUpdateResponseDto>> updateLocation(@Valid @RequestBody LocationUpdateDto locationDto) {
        // FIXED T03: Get authenticated user from security context
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication == null || !authentication.isAuthenticated()) {
            return ResponseEntity.status(401).body(ApiResponse.error("Authentication required"));
        }
        
        // FIXED T03: Validate that the authenticated user matches the userId in the request
        String authenticatedUsername = authentication.getName();
        User authenticatedUser = userService.findByUsername(authenticatedUsername)
                .orElseThrow(() -> new RuntimeException("User not found"));
        
        if (!authenticatedUser.getId().equals(locationDto.getUserId())) {
            return ResponseEntity.status(403).body(ApiResponse.error("You can only update your own location"));
        }
        
        LocationUpdate locationUpdate = locationService.saveLocationUpdate(locationDto);
        return ResponseEntity.ok(ApiResponse.success("Location updated successfully", 
                locationMapper.toLocationUpdateResponseDto(locationUpdate)));
    }
    
    @GetMapping("/ride/{rideId}")
    public ResponseEntity<ApiResponse<List<LocationUpdateResponseDto>>> getLocationUpdatesForRide(@PathVariable UUID rideId) {
        // FIXED T03: Require authentication for location data access
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication == null || !authentication.isAuthenticated()) {
            return ResponseEntity.status(401).body(ApiResponse.error("Authentication required"));
        }
        
        List<LocationUpdate> updates = locationService.getLocationUpdatesForRide(rideId);
        return ResponseEntity.ok(ApiResponse.success("Location updates retrieved successfully", 
                locationMapper.toLocationUpdateResponseDtoList(updates)));
    }
    
    @GetMapping("/user/{userId}")
    public ResponseEntity<ApiResponse<List<LocationUpdateResponseDto>>> getLocationUpdatesForUser(@PathVariable UUID userId) {
        // FIXED T03: Require authentication and validate user access
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication == null || !authentication.isAuthenticated()) {
            return ResponseEntity.status(401).body(ApiResponse.error("Authentication required"));
        }
        
        // FIXED T03: Validate that the authenticated user can access this user's location data
        String authenticatedUsername = authentication.getName();
        User authenticatedUser = userService.findByUsername(authenticatedUsername)
                .orElseThrow(() -> new RuntimeException("User not found"));
        
        if (!authenticatedUser.getId().equals(userId)) {
            return ResponseEntity.status(403).body(ApiResponse.error("You can only access your own location data"));
        }
        
        List<LocationUpdate> updates = locationService.getLocationUpdatesForUser(userId);
        return ResponseEntity.ok(ApiResponse.success("User location updates retrieved successfully", 
                locationMapper.toLocationUpdateResponseDtoList(updates)));
    }
    
    // Get locations of all group members during a ride
    @GetMapping("/group/{groupId}/ride/{rideId}")
    public ResponseEntity<ApiResponse<List<LocationUpdateResponseDto>>> getGroupLocationUpdatesForRide(
            @PathVariable UUID groupId, 
            @PathVariable UUID rideId) {
        // FIXED T03: Require authentication for group location access
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication == null || !authentication.isAuthenticated()) {
            return ResponseEntity.status(401).body(ApiResponse.error("Authentication required"));
        }
        
        List<LocationUpdate> updates = locationService.getGroupLocationUpdatesForRide(groupId, rideId);
        return ResponseEntity.ok(ApiResponse.success("Group location updates retrieved successfully", 
                locationMapper.toLocationUpdateResponseDtoList(updates)));
    }
    
    // Get current locations of all active group members
    @GetMapping("/group/{groupId}/current")
    public ResponseEntity<ApiResponse<List<LocationUpdateResponseDto>>> getCurrentGroupLocations(@PathVariable UUID groupId) {
        // FIXED T03: Require authentication for group location access
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication == null || !authentication.isAuthenticated()) {
            return ResponseEntity.status(401).body(ApiResponse.error("Authentication required"));
        }
        
        List<LocationUpdate> updates = locationService.getCurrentGroupLocations(groupId);
        return ResponseEntity.ok(ApiResponse.success("Current group locations retrieved successfully", 
                locationMapper.toLocationUpdateResponseDtoList(updates)));
    }
    
    // FIXED T13: Location data leakage – now requires authentication
    @GetMapping("/active")
    public ResponseEntity<ApiResponse<List<LocationUpdateResponseDto>>> getAllActiveLocationUpdates() {
        // FIXED T03: Require authentication for active location access
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication == null || !authentication.isAuthenticated()) {
            return ResponseEntity.status(401).body(ApiResponse.error("Authentication required"));
        }
        
        List<LocationUpdate> updates = locationService.getAllActiveLocationUpdates();
        return ResponseEntity.ok(ApiResponse.success("Active location updates retrieved successfully", 
                locationMapper.toLocationUpdateResponseDtoList(updates)));
    }
    
    @GetMapping("/nearby")
    public ResponseEntity<ApiResponse<List<LocationUpdateResponseDto>>> getNearbyLocationUpdates(@RequestParam Double latitude,
                                                    @RequestParam Double longitude,
                                                    @RequestParam(defaultValue = "1000") Double radius) {
        // FIXED T03: Require authentication for nearby location access
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication == null || !authentication.isAuthenticated()) {
            return ResponseEntity.status(401).body(ApiResponse.error("Authentication required"));
        }
        
        List<LocationUpdate> updates = locationService.getNearbyLocationUpdates(latitude, longitude, radius);
        return ResponseEntity.ok(ApiResponse.success("Nearby location updates retrieved successfully", 
                locationMapper.toLocationUpdateResponseDtoList(updates)));
    }
}
