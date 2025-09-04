package com.ridesync.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class GroupResponseDto {
    private Long id;
    private String name;
    private String description;
    private Long createdBy;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
    private List<GroupMemberResponseDto> members;
}

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
class GroupMemberResponseDto {
    private Long id;
    private Long userId;
    private String username;
    private String role;
    private LocalDateTime joinedAt;
}
