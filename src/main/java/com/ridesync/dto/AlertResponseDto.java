
package com.ridesync.dto;

import com.ridesync.model.AlertType;
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
public class AlertResponseDto {
    
    private UUID id;
    private AlertType type;
    private String message;
    private Boolean isRead;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
    private UUID rideId;
    private String rideName;
    private UUID userId;
    private String userName;
    private UUID deviceId;
    private String deviceName;
    private String deviceType;
}
