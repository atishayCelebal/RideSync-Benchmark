package com.ridesync.dto;

import com.ridesync.model.DeviceType;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.UUID;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class DeviceResponseDto {
    
    private UUID id;
    private String deviceName;
    private String deviceId;
    private DeviceType deviceType;
    private String osVersion;
    private String appVersion;
    private Double gpsAccuracy;
    private LocalDateTime lastSeen;
    private Boolean isActive;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
    private UUID userId;
    private String userName;
}
