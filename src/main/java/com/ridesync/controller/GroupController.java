package com.ridesync.controller;

import com.ridesync.dto.ApiResponse;
import com.ridesync.dto.GroupInviteDto;
import com.ridesync.dto.GroupResponseDto;
import com.ridesync.dto.SimpleResponseDto;
import com.ridesync.mapper.GroupMapper;
import com.ridesync.model.Group;
import com.ridesync.model.GroupMember;
import com.ridesync.model.GroupRole;
import com.ridesync.service.GroupService;
import com.ridesync.service.UserService;
import jakarta.validation.Valid;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/groups")
@CrossOrigin(origins = "*")
public class GroupController {
    
    @Autowired
    private GroupService groupService;
    
    @Autowired
    private UserService userService;
    
    @Autowired
    private GroupMapper groupMapper;
    
    @PostMapping
    public ResponseEntity<ApiResponse<GroupResponseDto>> createGroup(@RequestBody Map<String, String> groupRequest,
                                        @RequestHeader("X-User-Id") Long userId) {
        String name = groupRequest.get("name");
        String description = groupRequest.get("description");
        
        Group group = groupService.createGroup(name, description, userId);
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.success("Group created successfully", groupMapper.toGroupResponseDto(group)));
    }
    
    @GetMapping
    public ResponseEntity<ApiResponse<List<GroupResponseDto>>> getUserGroups(@RequestHeader("X-User-Id") Long userId) {
        List<Group> groups = groupService.getUserGroups(userId);
        return ResponseEntity.ok(ApiResponse.success("User groups retrieved successfully", 
                groupMapper.toGroupResponseDtoList(groups)));
    }
    
    @GetMapping("/{groupId}")
    public ResponseEntity<ApiResponse<GroupResponseDto>> getGroup(@PathVariable Long groupId) {
        Group group = groupService.findById(groupId)
                .orElseThrow(() -> new com.ridesync.exception.ResourceNotFoundException("Group", "id", groupId));
        return ResponseEntity.ok(ApiResponse.success("Group retrieved successfully", 
                groupMapper.toGroupResponseDto(group)));
    }
    
    @GetMapping("/{groupId}/members")
    public ResponseEntity<ApiResponse<List<GroupMember>>> getGroupMembers(@PathVariable Long groupId) {
        List<GroupMember> members = groupService.getGroupMembers(groupId);
        return ResponseEntity.ok(ApiResponse.success("Group members retrieved successfully", members));
    }
    
    // BUG T02: Invite via Email lacks Admin role check – Anyone can send invites
    @PostMapping("/{groupId}/invite")
    public ResponseEntity<ApiResponse<SimpleResponseDto>> sendGroupInvite(@PathVariable Long groupId,
                                           @Valid @RequestBody GroupInviteDto inviteDto,
                                           @RequestHeader("X-User-Id") Long senderId) {
        // BUG T02: No admin role check - anyone can send invites
        groupService.sendGroupInvite(inviteDto, senderId);
        
        SimpleResponseDto responseDto = SimpleResponseDto.builder()
                .message("Invitation sent successfully")
                .build();
        
        return ResponseEntity.ok(ApiResponse.success("Invitation sent successfully", responseDto));
    }
    
    @PostMapping("/{groupId}/members")
    public ResponseEntity<ApiResponse<SimpleResponseDto>> addMemberToGroup(@PathVariable Long groupId,
                                            @RequestBody Map<String, Object> memberRequest,
                                            @RequestHeader("X-User-Id") Long adminId) {
        Long userId = Long.valueOf(memberRequest.get("userId").toString());
        String roleStr = memberRequest.get("role").toString();
        GroupRole role = GroupRole.valueOf(roleStr.toUpperCase());
        
        GroupMember member = groupService.addMemberToGroup(groupId, userId, role);
        
        SimpleResponseDto responseDto = SimpleResponseDto.builder()
                .message("Member added successfully")
                .id(member.getId())
                .build();
        
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.success("Member added successfully", responseDto));
    }
}
