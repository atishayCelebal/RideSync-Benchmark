package com.ridesync.dto;

import com.ridesync.model.DeviceType;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class DeviceRequestDto {
    
    @NotBlank(message = "Device name is required")
    private String deviceName;
    
    @NotBlank(message = "Device ID is required")
    private String deviceId;
    
    @NotNull(message = "Device type is required")
    private DeviceType deviceType;
    
    private String osVersion;
    private String appVersion;
    private Double gpsAccuracy;
}
