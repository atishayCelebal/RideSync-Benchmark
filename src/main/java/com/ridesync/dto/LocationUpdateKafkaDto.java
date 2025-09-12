package com.ridesync.dto;

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
public class LocationUpdateKafkaDto {
    private UUID locationUpdateId;
    private UUID userId;
    private UUID rideId;
    private UUID groupId;
    private Double latitude;
    private Double longitude;
    private Double altitude;
    private Double speed;
    private Double heading;
    private Double accuracy;
    private LocalDateTime timestamp;
    private UUID deviceId;
}
