package com.ridesync.dto;

import com.fasterxml.jackson.annotation.JsonFormat;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import jakarta.validation.constraints.*;
import java.time.LocalDateTime;
import java.util.UUID;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class LocationUpdateDto {
    
    @NotNull(message = "User ID is required")
    private UUID userId;
    
    @NotNull(message = "Ride ID is required")
    private UUID rideId;
    
    @NotNull(message = "Latitude is required")
    @DecimalMin(value = "-90.0", message = "Latitude must be between -90 and 90")
    @DecimalMax(value = "90.0", message = "Latitude must be between -90 and 90")
    private Double latitude;
    
    @NotNull(message = "Longitude is required")
    @DecimalMin(value = "-180.0", message = "Longitude must be between -180 and 180")
    @DecimalMax(value = "180.0", message = "Longitude must be between -180 and 180")
    private Double longitude;
    
    @NotNull(message = "Timestamp is required")
    @JsonFormat(pattern = "yyyy-MM-dd'T'HH:mm:ss")
    private LocalDateTime timestamp;
    private Double altitude;
    private Double speed;
    private Double heading;

    @NotNull(message = "Device ID is required")
    private UUID deviceId;
    
    @DecimalMin(value = "0.0", message = "Accuracy must be non-negative")
    private Double accuracy;
    
    // Custom validation for timestamp (not too old, not in future)
    @AssertTrue(message = "Timestamp must be within reasonable range")
    public boolean isValidTimestamp() {
        if (timestamp == null) return false;
        LocalDateTime now = LocalDateTime.now();
        LocalDateTime oneYearAgo = now.minusYears(1);
        LocalDateTime oneHourFromNow = now.plusHours(1);
        return timestamp.isAfter(oneYearAgo) && timestamp.isBefore(oneHourFromNow);
    }
}