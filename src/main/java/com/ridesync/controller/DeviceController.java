package com.ridesync.controller;

import com.ridesync.dto.ApiResponse;
import com.ridesync.dto.DeviceRequestDto;
import com.ridesync.dto.DeviceResponseDto;
import com.ridesync.dto.SimpleResponseDto;
import com.ridesync.mapper.DeviceMapper;
import com.ridesync.model.Device;
import com.ridesync.service.DeviceService;
import com.ridesync.util.SecurityUtil;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/v1/devices")
@RequiredArgsConstructor
public class DeviceController {
    
    private final DeviceService deviceService;
    private final DeviceMapper deviceMapper;
    
    @PostMapping
    public ResponseEntity<ApiResponse<DeviceResponseDto>> registerDevice(@Valid @RequestBody DeviceRequestDto deviceRequest) {
        UUID userId = SecurityUtil.getCurrentUserId();
        
        Device device = deviceService.registerDevice(
            deviceRequest.getDeviceName(),
            deviceRequest.getDeviceId(),
            deviceRequest.getDeviceType(),
            deviceRequest.getOsVersion(),
            deviceRequest.getAppVersion(),
            deviceRequest.getGpsAccuracy(),
            userId
        );
        
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.success("Device registered successfully", deviceMapper.toDeviceResponseDto(device)));
    }
    
    @GetMapping
    public ResponseEntity<ApiResponse<List<DeviceResponseDto>>> getUserDevices() {
        UUID userId = SecurityUtil.getCurrentUserId();
        List<Device> devices = deviceService.getUserDevices(userId);
        return ResponseEntity.ok(ApiResponse.success("User devices retrieved successfully", 
                deviceMapper.toDeviceResponseDtoList(devices)));
    }
    
    @GetMapping("/{deviceId}")
    public ResponseEntity<ApiResponse<DeviceResponseDto>> getDevice(@PathVariable UUID deviceId) {
        Device device = deviceService.findById(deviceId)
                .orElseThrow(() -> new com.ridesync.exception.ResourceNotFoundException("Device", "id", deviceId));
        return ResponseEntity.ok(ApiResponse.success("Device retrieved successfully", 
                deviceMapper.toDeviceResponseDto(device)));
    }
    
    @PutMapping("/{deviceId}")
    public ResponseEntity<ApiResponse<DeviceResponseDto>> updateDevice(@PathVariable UUID deviceId, 
                                                                      @Valid @RequestBody DeviceRequestDto deviceRequest) {
        Device device = deviceService.updateDevice(deviceId, deviceRequest);
        return ResponseEntity.ok(ApiResponse.success("Device updated successfully", 
                deviceMapper.toDeviceResponseDto(device)));
    }
    
    @DeleteMapping("/{deviceId}")
    public ResponseEntity<ApiResponse<SimpleResponseDto>> removeDevice(@PathVariable UUID deviceId) {
        deviceService.removeDevice(deviceId);
        SimpleResponseDto response = SimpleResponseDto.builder()
                .message("Device removed successfully")
                .build();
        return ResponseEntity.ok(ApiResponse.success("Device removed successfully", response));
    }
    
    @PutMapping("/{deviceId}/activate")
    public ResponseEntity<ApiResponse<DeviceResponseDto>> activateDevice(@PathVariable UUID deviceId) {
        Device device = deviceService.activateDevice(deviceId);
        return ResponseEntity.ok(ApiResponse.success("Device activated successfully", 
                deviceMapper.toDeviceResponseDto(device)));
    }
    
    @PutMapping("/{deviceId}/deactivate")
    public ResponseEntity<ApiResponse<DeviceResponseDto>> deactivateDevice(@PathVariable UUID deviceId) {
        Device device = deviceService.deactivateDevice(deviceId);
        return ResponseEntity.ok(ApiResponse.success("Device deactivated successfully", 
                deviceMapper.toDeviceResponseDto(device)));
    }
    
    @PutMapping("/{deviceId}/heartbeat")
    public ResponseEntity<ApiResponse<DeviceResponseDto>> updateDeviceHeartbeat(@PathVariable UUID deviceId) {
        Device device = deviceService.updateLastSeen(deviceId);
        return ResponseEntity.ok(ApiResponse.success("Device heartbeat updated successfully", 
                deviceMapper.toDeviceResponseDto(device)));
    }
}
