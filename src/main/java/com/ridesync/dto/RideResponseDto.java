package com.ridesync.dto;

import com.ridesync.model.RideStatus;
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
public class RideResponseDto {
    
    private UUID id;
    private String name;
    private String description;
    private RideStatus status;
    private Boolean isActive;
    private LocalDateTime startTime;
    private LocalDateTime endTime;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
    private UUID groupId;
    private String groupName;
    private UUID createdBy;
    private String createdByName;
}
