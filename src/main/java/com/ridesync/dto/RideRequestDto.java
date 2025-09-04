package com.ridesync.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.UUID;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class RideRequestDto {
    
    @NotBlank(message = "Ride name is required")
    private String name;
    
    private String description;
    
    @NotNull(message = "Group ID is required")
    private UUID groupId;
}
