package com.ridesync.controller;

import com.ridesync.dto.GroupInviteDto;
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

import java.util.HashMap;
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
    
    @PostMapping
    public ResponseEntity<?> createGroup(@RequestBody Map<String, String> groupRequest,
                                        @RequestHeader("X-User-Id") Long userId) {
        try {
            String name = groupRequest.get("name");
            String description = groupRequest.get("description");
            
            Group group = groupService.createGroup(name, description, userId);
            
            Map<String, Object> response = new HashMap<>();
            response.put("message", "Group created successfully");
            response.put("groupId", group.getId());
            response.put("groupName", group.getName());
            
            return ResponseEntity.status(HttpStatus.CREATED).body(response);
        } catch (Exception e) {
            Map<String, String> error = new HashMap<>();
            error.put("error", "Failed to create group: " + e.getMessage());
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(error);
        }
    }
    
    @GetMapping
    public ResponseEntity<?> getUserGroups(@RequestHeader("X-User-Id") Long userId) {
        try {
            List<Group> groups = groupService.getUserGroups(userId);
            return ResponseEntity.ok(groups);
        } catch (Exception e) {
            Map<String, String> error = new HashMap<>();
            error.put("error", "Failed to fetch groups: " + e.getMessage());
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(error);
        }
    }
    
    @GetMapping("/{groupId}")
    public ResponseEntity<?> getGroup(@PathVariable Long groupId) {
        try {
            Group group = groupService.findById(groupId).orElse(null);
            if (group == null) {
                return ResponseEntity.status(HttpStatus.NOT_FOUND)
                        .body(Map.of("error", "Group not found"));
            }
            return ResponseEntity.ok(group);
        } catch (Exception e) {
            Map<String, String> error = new HashMap<>();
            error.put("error", "Failed to fetch group: " + e.getMessage());
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(error);
        }
    }
    
    @GetMapping("/{groupId}/members")
    public ResponseEntity<?> getGroupMembers(@PathVariable Long groupId) {
        try {
            List<GroupMember> members = groupService.getGroupMembers(groupId);
            return ResponseEntity.ok(members);
        } catch (Exception e) {
            Map<String, String> error = new HashMap<>();
            error.put("error", "Failed to fetch group members: " + e.getMessage());
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(error);
        }
    }
    
    // BUG T02: Invite via Email lacks Admin role check – Anyone can send invites
    @PostMapping("/{groupId}/invite")
    public ResponseEntity<?> sendGroupInvite(@PathVariable Long groupId,
                                           @Valid @RequestBody GroupInviteDto inviteDto,
                                           @RequestHeader("X-User-Id") Long senderId) {
        try {
            // BUG T02: No admin role check - anyone can send invites
            groupService.sendGroupInvite(inviteDto, senderId);
            
            Map<String, String> response = new HashMap<>();
            response.put("message", "Invitation sent successfully");
            
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            Map<String, String> error = new HashMap<>();
            error.put("error", "Failed to send invitation: " + e.getMessage());
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(error);
        }
    }
    
    @PostMapping("/{groupId}/members")
    public ResponseEntity<?> addMemberToGroup(@PathVariable Long groupId,
                                            @RequestBody Map<String, Object> memberRequest,
                                            @RequestHeader("X-User-Id") Long adminId) {
        try {
            Long userId = Long.valueOf(memberRequest.get("userId").toString());
            String roleStr = memberRequest.get("role").toString();
            GroupRole role = GroupRole.valueOf(roleStr.toUpperCase());
            
            GroupMember member = groupService.addMemberToGroup(groupId, userId, role);
            
            Map<String, Object> response = new HashMap<>();
            response.put("message", "Member added successfully");
            response.put("memberId", member.getId());
            
            return ResponseEntity.status(HttpStatus.CREATED).body(response);
        } catch (Exception e) {
            Map<String, String> error = new HashMap<>();
            error.put("error", "Failed to add member: " + e.getMessage());
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(error);
        }
    }
}
