package com.ridesync.controller;

import com.ridesync.dto.ApiResponse;
import com.ridesync.dto.AlertResponseDto;
import com.ridesync.dto.SimpleResponseDto;
import com.ridesync.mapper.AlertMapper;
import com.ridesync.model.Alert;
import com.ridesync.service.AlertService;
import com.ridesync.util.SecurityUtil;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/v1/alerts")
@RequiredArgsConstructor
public class AlertController {
    
    private final AlertService alertService;
    private final AlertMapper alertMapper;
    
    @GetMapping
    public ResponseEntity<ApiResponse<List<AlertResponseDto>>> getUserAlerts() {
        UUID userId = SecurityUtil.getCurrentUserId();
        List<Alert> alerts = alertService.getUserAlerts(userId);
        return ResponseEntity.ok(ApiResponse.success("User alerts retrieved successfully", 
                alertMapper.toAlertResponseDtoList(alerts)));
    }
    
    @GetMapping("/{alertId}")
    public ResponseEntity<ApiResponse<AlertResponseDto>> getAlert(@PathVariable UUID alertId) {
        Alert alert = alertService.findById(alertId)
                .orElseThrow(() -> new com.ridesync.exception.ResourceNotFoundException("Alert", "id", alertId));
        return ResponseEntity.ok(ApiResponse.success("Alert retrieved successfully", 
                alertMapper.toAlertResponseDto(alert)));
    }
    
    @PutMapping("/{alertId}/read")
    public ResponseEntity<ApiResponse<SimpleResponseDto>> markAlertAsRead(@PathVariable UUID alertId) {
        alertService.markAsRead(alertId);
        SimpleResponseDto response = SimpleResponseDto.builder()
                .message("Alert marked as read")
                .build();
        return ResponseEntity.ok(ApiResponse.success("Alert marked as read", response));
    }
    
    @DeleteMapping("/{alertId}")
    public ResponseEntity<ApiResponse<SimpleResponseDto>> deleteAlert(@PathVariable UUID alertId) {
        alertService.deleteAlert(alertId);
        SimpleResponseDto response = SimpleResponseDto.builder()
                .message("Alert deleted successfully")
                .build();
        return ResponseEntity.ok(ApiResponse.success("Alert deleted successfully", response));
    }
    
    @GetMapping("/ride/{rideId}")
    public ResponseEntity<ApiResponse<List<AlertResponseDto>>> getRideAlerts(@PathVariable UUID rideId) {
        List<Alert> alerts = alertService.getRideAlerts(rideId);
        return ResponseEntity.ok(ApiResponse.success("Ride alerts retrieved successfully", 
                alertMapper.toAlertResponseDtoList(alerts)));
    }
    
    @GetMapping("/group/{groupId}")
    public ResponseEntity<ApiResponse<List<AlertResponseDto>>> getGroupAlerts(@PathVariable UUID groupId) {
        List<Alert> alerts = alertService.getGroupAlerts(groupId);
        return ResponseEntity.ok(ApiResponse.success("Group alerts retrieved successfully", 
                alertMapper.toAlertResponseDtoList(alerts)));
    }
    
    @GetMapping("/unread")
    public ResponseEntity<ApiResponse<List<AlertResponseDto>>> getUnreadAlerts() {
        UUID userId = SecurityUtil.getCurrentUserId();
        List<Alert> alerts = alertService.getUnreadAlerts(userId);
        return ResponseEntity.ok(ApiResponse.success("Unread alerts retrieved successfully", 
                alertMapper.toAlertResponseDtoList(alerts)));
    }
}
