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
        try {
            // BUG T03: No authentication/authorization check
            // BUG T03: No validation that userId in token matches payload
            LocationUpdate locationUpdate = locationService.saveLocationUpdate(locationDto);
            return ResponseEntity.ok(ApiResponse.success("Location updated successfully", 
                    locationMapper.toLocationUpdateResponseDto(locationUpdate)));
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                    .body(ApiResponse.error("Failed to update location: " + e.getMessage(), "LOCATION_UPDATE_ERROR"));
        }
    }
    
    @GetMapping("/ride/{rideId}")
    public ResponseEntity<ApiResponse<List<LocationUpdateResponseDto>>> getLocationUpdatesForRide(@PathVariable Long rideId) {
        try {
            List<LocationUpdate> updates = locationService.getLocationUpdatesForRide(rideId);
            return ResponseEntity.ok(ApiResponse.success("Location updates retrieved successfully", 
                    locationMapper.toLocationUpdateResponseDtoList(updates)));
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(ApiResponse.error("Failed to fetch location updates: " + e.getMessage(), "FETCH_LOCATION_ERROR"));
        }
    }
    
    @GetMapping("/user/{userId}")
    public ResponseEntity<ApiResponse<List<LocationUpdateResponseDto>>> getLocationUpdatesForUser(@PathVariable Long userId) {
        try {
            List<LocationUpdate> updates = locationService.getLocationUpdatesForUser(userId);
            return ResponseEntity.ok(ApiResponse.success("User location updates retrieved successfully", 
                    locationMapper.toLocationUpdateResponseDtoList(updates)));
        } catch (Exception e) {
            ApiResponse<List<LocationUpdateResponseDto>> response = ApiResponse.error("Failed to fetch location updates: " + e.getMessage(), "FETCH_USER_LOCATION_ERROR");
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(response);
        }
    }
    
    // BUG T13: Location data leakage – unrestricted API query
    @GetMapping("/active")
    public ResponseEntity<ApiResponse<List<LocationUpdateResponseDto>>> getAllActiveLocationUpdates() {
        try {
            // BUG T13: Returns all active locations without group filtering
            List<LocationUpdate> updates = locationService.getAllActiveLocationUpdates();
            List<LocationUpdateResponseDto> responseDtos = locationMapper.toLocationUpdateResponseDtoList(updates);
            
            ApiResponse<List<LocationUpdateResponseDto>> response = ApiResponse.success("Active location updates retrieved successfully", responseDtos);
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            ApiResponse<List<LocationUpdateResponseDto>> response = ApiResponse.error("Failed to fetch active location updates: " + e.getMessage(), "FETCH_ACTIVE_LOCATION_ERROR");
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(response);
        }
    }
    
    @GetMapping("/nearby")
    public ResponseEntity<ApiResponse<List<LocationUpdateResponseDto>>> getNearbyLocationUpdates(@RequestParam Double latitude,
                                                    @RequestParam Double longitude,
                                                    @RequestParam(defaultValue = "1000") Double radius) {
        try {
            List<LocationUpdate> updates = locationService.getNearbyLocationUpdates(latitude, longitude, radius);
            List<LocationUpdateResponseDto> responseDtos = locationMapper.toLocationUpdateResponseDtoList(updates);
            
            ApiResponse<List<LocationUpdateResponseDto>> response = ApiResponse.success("Nearby location updates retrieved successfully", responseDtos);
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            ApiResponse<List<LocationUpdateResponseDto>> response = ApiResponse.error("Failed to fetch nearby location updates: " + e.getMessage(), "FETCH_NEARBY_LOCATION_ERROR");
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(response);
        }
    }
}
