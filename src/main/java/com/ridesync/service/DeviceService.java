package com.ridesync.service;

import com.ridesync.dto.DeviceRequestDto;
import com.ridesync.exception.ResourceNotFoundException;
import com.ridesync.model.Device;
import com.ridesync.model.DeviceType;
import com.ridesync.model.User;
import com.ridesync.repository.DeviceRepository;
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
public class DeviceService {
    
    private final DeviceRepository deviceRepository;
    private final UserRepository userRepository;
    
    public Device registerDevice(String deviceName, String deviceId, DeviceType deviceType, 
                                String osVersion, String appVersion, Double gpsAccuracy, UUID userId) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new ResourceNotFoundException("User", "id", userId));
        
        Device device = Device.builder()
                .deviceName(deviceName)
                .deviceId(deviceId)
                .deviceType(deviceType)
                .osVersion(osVersion)
                .appVersion(appVersion)
                .gpsAccuracy(gpsAccuracy)
                .lastSeen(LocalDateTime.now())
                .isActive(true)
                .user(user)
                .build();
        
        return deviceRepository.save(device);
    }
    
    public List<Device> getUserDevices(UUID userId) {
        return deviceRepository.findByUserIdAndIsActiveTrue(userId);
    }
    
    public Optional<Device> findById(UUID deviceId) {
        return deviceRepository.findById(deviceId);
    }
    
    public Device updateDevice(UUID deviceId, DeviceRequestDto deviceRequest) {
        Device device = deviceRepository.findById(deviceId)
                .orElseThrow(() -> new ResourceNotFoundException("Device", "id", deviceId));
        
        device.setDeviceName(deviceRequest.getDeviceName());
        device.setOsVersion(deviceRequest.getOsVersion());
        device.setAppVersion(deviceRequest.getAppVersion());
        device.setGpsAccuracy(deviceRequest.getGpsAccuracy());
        device.setUpdatedAt(LocalDateTime.now());
        
        return deviceRepository.save(device);
    }
    
    public void removeDevice(UUID deviceId) {
        Device device = deviceRepository.findById(deviceId)
                .orElseThrow(() -> new ResourceNotFoundException("Device", "id", deviceId));
        
        device.setIsActive(false);
        device.setUpdatedAt(LocalDateTime.now());
        deviceRepository.save(device);
    }
    
    public Device activateDevice(UUID deviceId) {
        Device device = deviceRepository.findById(deviceId)
                .orElseThrow(() -> new ResourceNotFoundException("Device", "id", deviceId));
        
        device.setIsActive(true);
        device.setUpdatedAt(LocalDateTime.now());
        
        return deviceRepository.save(device);
    }
    
    public Device deactivateDevice(UUID deviceId) {
        Device device = deviceRepository.findById(deviceId)
                .orElseThrow(() -> new ResourceNotFoundException("Device", "id", deviceId));
        
        device.setIsActive(false);
        device.setUpdatedAt(LocalDateTime.now());
        
        return deviceRepository.save(device);
    }
    
    public Device updateLastSeen(UUID deviceId) {
        Device device = deviceRepository.findById(deviceId)
                .orElseThrow(() -> new ResourceNotFoundException("Device", "id", deviceId));
        
        device.setLastSeen(LocalDateTime.now());
        device.setUpdatedAt(LocalDateTime.now());
        
        return deviceRepository.save(device);
    }
}
