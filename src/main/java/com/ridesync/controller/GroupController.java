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
        try {
            String name = groupRequest.get("name");
            String description = groupRequest.get("description");
            
            Group group = groupService.createGroup(name, description, userId);
            GroupResponseDto responseDto = groupMapper.toGroupResponseDto(group);
            
            ApiResponse<GroupResponseDto> response = ApiResponse.success("Group created successfully", responseDto);
            return ResponseEntity.status(HttpStatus.CREATED).body(response);
        } catch (Exception e) {
            ApiResponse<GroupResponseDto> response = ApiResponse.error("Failed to create group: " + e.getMessage(), "CREATE_GROUP_ERROR");
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(response);
        }
    }
    
    @GetMapping
    public ResponseEntity<ApiResponse<List<GroupResponseDto>>> getUserGroups(@RequestHeader("X-User-Id") Long userId) {
        try {
            List<Group> groups = groupService.getUserGroups(userId);
            List<GroupResponseDto> responseDtos = groupMapper.toGroupResponseDtoList(groups);
            
            ApiResponse<List<GroupResponseDto>> response = ApiResponse.success("User groups retrieved successfully", responseDtos);
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            ApiResponse<List<GroupResponseDto>> response = ApiResponse.error("Failed to fetch groups: " + e.getMessage(), "FETCH_GROUPS_ERROR");
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(response);
        }
    }
    
    @GetMapping("/{groupId}")
    public ResponseEntity<ApiResponse<GroupResponseDto>> getGroup(@PathVariable Long groupId) {
        try {
            Group group = groupService.findById(groupId).orElse(null);
            if (group == null) {
                ApiResponse<GroupResponseDto> response = ApiResponse.error("Group not found", "GROUP_NOT_FOUND");
                return ResponseEntity.status(HttpStatus.NOT_FOUND).body(response);
            }
            GroupResponseDto responseDto = groupMapper.toGroupResponseDto(group);
            ApiResponse<GroupResponseDto> response = ApiResponse.success("Group retrieved successfully", responseDto);
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            ApiResponse<GroupResponseDto> response = ApiResponse.error("Failed to fetch group: " + e.getMessage(), "FETCH_GROUP_ERROR");
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(response);
        }
    }
    
    @GetMapping("/{groupId}/members")
    public ResponseEntity<ApiResponse<List<GroupMember>>> getGroupMembers(@PathVariable Long groupId) {
        try {
            List<GroupMember> members = groupService.getGroupMembers(groupId);
            ApiResponse<List<GroupMember>> response = ApiResponse.success("Group members retrieved successfully", members);
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            ApiResponse<List<GroupMember>> response = ApiResponse.error("Failed to fetch group members: " + e.getMessage(), "FETCH_MEMBERS_ERROR");
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(response);
        }
    }
    
    // BUG T02: Invite via Email lacks Admin role check – Anyone can send invites
    @PostMapping("/{groupId}/invite")
    public ResponseEntity<ApiResponse<SimpleResponseDto>> sendGroupInvite(@PathVariable Long groupId,
                                           @Valid @RequestBody GroupInviteDto inviteDto,
                                           @RequestHeader("X-User-Id") Long senderId) {
        try {
            // BUG T02: No admin role check - anyone can send invites
            groupService.sendGroupInvite(inviteDto, senderId);
            
            SimpleResponseDto responseDto = SimpleResponseDto.builder()
                    .message("Invitation sent successfully")
                    .build();
            
            ApiResponse<SimpleResponseDto> response = ApiResponse.success("Invitation sent successfully", responseDto);
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            ApiResponse<SimpleResponseDto> response = ApiResponse.error("Failed to send invitation: " + e.getMessage(), "SEND_INVITE_ERROR");
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(response);
        }
    }
    
    @PostMapping("/{groupId}/members")
    public ResponseEntity<ApiResponse<SimpleResponseDto>> addMemberToGroup(@PathVariable Long groupId,
                                            @RequestBody Map<String, Object> memberRequest,
                                            @RequestHeader("X-User-Id") Long adminId) {
        try {
            Long userId = Long.valueOf(memberRequest.get("userId").toString());
            String roleStr = memberRequest.get("role").toString();
            GroupRole role = GroupRole.valueOf(roleStr.toUpperCase());
            
            GroupMember member = groupService.addMemberToGroup(groupId, userId, role);
            
            SimpleResponseDto responseDto = SimpleResponseDto.builder()
                    .message("Member added successfully")
                    .id(member.getId())
                    .build();
            
            ApiResponse<SimpleResponseDto> response = ApiResponse.success("Member added successfully", responseDto);
            return ResponseEntity.status(HttpStatus.CREATED).body(response);
        } catch (Exception e) {
            ApiResponse<SimpleResponseDto> response = ApiResponse.error("Failed to add member: " + e.getMessage(), "ADD_MEMBER_ERROR");
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(response);
        }
    }
}
