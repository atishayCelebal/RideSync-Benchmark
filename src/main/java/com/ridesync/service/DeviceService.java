package com.ridesync.service;

import com.ridesync.dto.DeviceRequestDto;
import com.ridesync.model.Device;
import com.ridesync.model.DeviceType;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface DeviceService {
    
    Device registerDevice(String deviceName, String deviceId, DeviceType deviceType, 
                         String osVersion, String appVersion, Double gpsAccuracy, UUID userId);
    
    List<Device> getUserDevices(UUID userId);
    
    Optional<Device> findById(UUID deviceId);
    
    Device updateDevice(UUID deviceId, DeviceRequestDto deviceRequest);
    
    void removeDevice(UUID deviceId);
    
    Device activateDevice(UUID deviceId);
    
    Device deactivateDevice(UUID deviceId);
    
    Device updateLastSeen(UUID deviceId);
}