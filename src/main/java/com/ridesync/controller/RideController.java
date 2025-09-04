package com.ridesync.controller;

import com.ridesync.dto.ApiResponse;
import com.ridesync.dto.RideRequestDto;
import com.ridesync.dto.RideResponseDto;
import com.ridesync.dto.SimpleResponseDto;
import com.ridesync.mapper.RideMapper;
import com.ridesync.model.Ride;
import com.ridesync.service.RideService;
import com.ridesync.util.SecurityUtil;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/v1/rides")
@RequiredArgsConstructor
public class RideController {
    
    private final RideService rideService;
    private final RideMapper rideMapper;
    
    @PostMapping
    public ResponseEntity<ApiResponse<RideResponseDto>> createRide(@Valid @RequestBody RideRequestDto rideRequest) {
        UUID userId = SecurityUtil.getCurrentUserId();
        
        Ride ride = rideService.createRide(
            rideRequest.getName(), 
            rideRequest.getDescription(), 
            rideRequest.getGroupId(), 
            userId
        );
        
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.success("Ride created successfully", rideMapper.toRideResponseDto(ride)));
    }
    
    @GetMapping
    public ResponseEntity<ApiResponse<List<RideResponseDto>>> getUserRides() {
        UUID userId = SecurityUtil.getCurrentUserId();
        List<Ride> rides = rideService.getRidesByUser(userId);
        return ResponseEntity.ok(ApiResponse.success("User rides retrieved successfully", 
                rideMapper.toRideResponseDtoList(rides)));
    }
    
    @GetMapping("/{rideId}")
    public ResponseEntity<ApiResponse<RideResponseDto>> getRide(@PathVariable UUID rideId) {
        Ride ride = rideService.findById(rideId)
                .orElseThrow(() -> new com.ridesync.exception.ResourceNotFoundException("Ride", "id", rideId));
        return ResponseEntity.ok(ApiResponse.success("Ride retrieved successfully", 
                rideMapper.toRideResponseDto(ride)));
    }
    
    @PutMapping("/{rideId}")
    public ResponseEntity<ApiResponse<RideResponseDto>> updateRide(@PathVariable UUID rideId, 
                                                                  @Valid @RequestBody RideRequestDto rideRequest) {
        Ride ride = rideService.updateRide(rideId, rideRequest);
        return ResponseEntity.ok(ApiResponse.success("Ride updated successfully", 
                rideMapper.toRideResponseDto(ride)));
    }
    
    @DeleteMapping("/{rideId}")
    public ResponseEntity<ApiResponse<SimpleResponseDto>> deleteRide(@PathVariable UUID rideId) {
        rideService.deleteRide(rideId);
        SimpleResponseDto response = SimpleResponseDto.builder()
                .message("Ride deleted successfully")
                .build();
        return ResponseEntity.ok(ApiResponse.success("Ride deleted successfully", response));
    }
    
    @PostMapping("/{rideId}/start")
    public ResponseEntity<ApiResponse<RideResponseDto>> startRide(@PathVariable UUID rideId) {
        Ride ride = rideService.startRide(rideId);
        return ResponseEntity.ok(ApiResponse.success("Ride started successfully", 
                rideMapper.toRideResponseDto(ride)));
    }
    
    @PostMapping("/{rideId}/end")
    public ResponseEntity<ApiResponse<RideResponseDto>> endRide(@PathVariable UUID rideId) {
        Ride ride = rideService.endRide(rideId);
        return ResponseEntity.ok(ApiResponse.success("Ride ended successfully", 
                rideMapper.toRideResponseDto(ride)));
    }
    
    @PostMapping("/{rideId}/pause")
    public ResponseEntity<ApiResponse<RideResponseDto>> pauseRide(@PathVariable UUID rideId) {
        Ride ride = rideService.pauseRide(rideId);
        return ResponseEntity.ok(ApiResponse.success("Ride paused successfully", 
                rideMapper.toRideResponseDto(ride)));
    }
    
    @PostMapping("/{rideId}/resume")
    public ResponseEntity<ApiResponse<RideResponseDto>> resumeRide(@PathVariable UUID rideId) {
        Ride ride = rideService.resumeRide(rideId);
        return ResponseEntity.ok(ApiResponse.success("Ride resumed successfully", 
                rideMapper.toRideResponseDto(ride)));
    }
    
    @GetMapping("/group/{groupId}")
    public ResponseEntity<ApiResponse<List<RideResponseDto>>> getGroupRides(@PathVariable UUID groupId) {
        List<Ride> rides = rideService.getRidesByGroup(groupId);
        return ResponseEntity.ok(ApiResponse.success("Group rides retrieved successfully", 
                rideMapper.toRideResponseDtoList(rides)));
    }
}
