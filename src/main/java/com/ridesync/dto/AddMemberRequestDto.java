package com.ridesync.dto;

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
public class AddMemberRequestDto {
    
    @NotNull(message = "User ID is required")
    private UUID userId;
    
    @NotNull(message = "Role is required")
    private String role;
}
