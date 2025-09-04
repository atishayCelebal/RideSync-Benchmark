package com.ridesync.dto;

import jakarta.validation.constraints.NotNull;
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
public class LocationUpdateDto {
    
    @NotNull(message = "User ID is required")
    private UUID userId;
    
    @NotNull(message = "Ride ID is required")
    private UUID rideId;
    
    @NotNull(message = "Latitude is required")
    private Double latitude;
    
    @NotNull(message = "Longitude is required")
    private Double longitude;
    
    private Double altitude;
    private Double speed;
    private Double heading;
    private Double accuracy;
    private UUID deviceId; // Now using UUID instead of String
    private LocalDateTime timestamp;
    
    // Custom constructor for convenience
    public LocationUpdateDto(UUID userId, UUID rideId, Double latitude, Double longitude) {
        this.userId = userId;
        this.rideId = rideId;
        this.latitude = latitude;
        this.longitude = longitude;
        this.timestamp = LocalDateTime.now();
    }
}
