package com.ridesync.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class GroupResponseDto {
    private UUID id;
    private String name;
    private String description;
    private UUID createdBy;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
    private List<GroupMemberResponseDto> members;
}

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
class GroupMemberResponseDto {
    private UUID id;
    private UUID userId;
    private String username;
    private String role;
    private LocalDateTime joinedAt;
}
