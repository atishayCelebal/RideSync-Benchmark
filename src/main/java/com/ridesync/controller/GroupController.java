package com.ridesync.controller;

import com.ridesync.dto.AddMemberRequestDto;
import com.ridesync.dto.ApiResponse;
import com.ridesync.dto.GroupInviteDto;
import com.ridesync.dto.GroupRequestDto;
import com.ridesync.dto.GroupResponseDto;
import com.ridesync.dto.SimpleResponseDto;
import com.ridesync.mapper.GroupMapper;
import com.ridesync.model.Group;
import com.ridesync.model.GroupMember;
import com.ridesync.model.GroupRole;
import com.ridesync.model.User;
import com.ridesync.service.GroupService;
import com.ridesync.util.SecurityUtil;
import java.util.UUID;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/groups")
@CrossOrigin(origins = "*")
@RequiredArgsConstructor
public class GroupController {
    
    private final GroupService groupService;
    private final GroupMapper groupMapper;
    
    @PostMapping
    public ResponseEntity<ApiResponse<GroupResponseDto>> createGroup(@Valid @RequestBody GroupRequestDto groupRequest) {
        User admin = SecurityUtil.getCurrentUser();
        
        Group group = groupService.createGroup(groupRequest.getName(), groupRequest.getDescription(), admin);
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.success("Group created successfully", groupMapper.toGroupResponseDto(group)));
    }
    
    @GetMapping
    public ResponseEntity<ApiResponse<List<GroupResponseDto>>> getUserGroups() {
        UUID userId = SecurityUtil.getCurrentUserId();
        List<Group> groups = groupService.getUserGroups(userId);
        return ResponseEntity.ok(ApiResponse.success("User groups retrieved successfully", 
                groupMapper.toGroupResponseDtoList(groups)));
    }
    
    @GetMapping("/{groupId}")
    public ResponseEntity<ApiResponse<GroupResponseDto>> getGroup(@PathVariable UUID groupId) {
        Group group = groupService.findById(groupId)
                .orElseThrow(() -> new com.ridesync.exception.ResourceNotFoundException("Group", "id", groupId));
        return ResponseEntity.ok(ApiResponse.success("Group retrieved successfully", 
                groupMapper.toGroupResponseDto(group)));
    }
    
    @GetMapping("/{groupId}/members")
    public ResponseEntity<ApiResponse<List<GroupMember>>> getGroupMembers(@PathVariable UUID groupId) {
        List<GroupMember> members = groupService.getGroupMembers(groupId);
        return ResponseEntity.ok(ApiResponse.success("Group members retrieved successfully", members));
    }
    
    // BUG T02: Invite via Email lacks Admin role check – Anyone can send invites
    @PostMapping("/{groupId}/invite")
    public ResponseEntity<ApiResponse<SimpleResponseDto>> sendGroupInvite(@PathVariable UUID groupId,
                                           @Valid @RequestBody GroupInviteDto inviteDto) {
        UUID senderId = SecurityUtil.getCurrentUserId();
        
        // BUG T02: No admin role check - anyone can send invites
        groupService.sendGroupInvite(inviteDto, senderId);
        
        SimpleResponseDto responseDto = SimpleResponseDto.builder()
                .message("Invitation sent successfully")
                .build();
        
        return ResponseEntity.ok(ApiResponse.success("Invitation sent successfully", responseDto));
    }
    
    @PostMapping("/{groupId}/members")
    public ResponseEntity<ApiResponse<SimpleResponseDto>> addMemberToGroup(@PathVariable UUID groupId,
                                            @Valid @RequestBody AddMemberRequestDto memberRequest) {
        GroupRole role = GroupRole.valueOf(memberRequest.getRole().toUpperCase());
        
        GroupMember member = groupService.addMemberToGroup(groupId, memberRequest.getUserId(), role);
        
        SimpleResponseDto responseDto = SimpleResponseDto.builder()
                .message("Member added successfully")
                .id(member.getId())
                .build();
        
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.success("Member added successfully", responseDto));
    }
}
