package com.ridesync.service.impl;

import com.ridesync.dto.GroupInviteDto;
import com.ridesync.exception.AuthorizationException;
import com.ridesync.exception.ResourceNotFoundException;
import com.ridesync.model.Group;
import com.ridesync.model.GroupMember;
import com.ridesync.model.GroupRole;
import com.ridesync.model.User;
import com.ridesync.repository.GroupMemberRepository;
import com.ridesync.repository.GroupRepository;
import com.ridesync.repository.UserRepository;
import com.ridesync.service.GroupService;
import com.ridesync.dto.GroupRequestDto;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Service
@Transactional
@RequiredArgsConstructor
public class GroupServiceImpl implements GroupService {
    
    private final GroupRepository groupRepository;
    private final GroupMemberRepository groupMemberRepository;
    private final UserRepository userRepository;
    
    public Group createGroup(GroupRequestDto groupRequest, User admin) {
        Group group = Group.builder()
                .name(groupRequest.getName())
                .description(groupRequest.getDescription())
                .admin(admin)
                .isActive(true)
                .build();
        
        Group savedGroup = groupRepository.save(group);
        
        // Add admin as first member
        GroupMember adminMember = GroupMember.builder()
                .group(savedGroup)
                .user(admin)
                .role(GroupRole.ADMIN)
                .joinedAt(LocalDateTime.now())
                .build();
        
        groupMemberRepository.save(adminMember);
        
        return savedGroup;
    }
    
    public List<Group> getUserGroups(UUID userId) {
        return groupRepository.findByUserIdAndIsActiveTrue(userId);
    }
    
    public Optional<Group> findById(UUID groupId) {
        return groupRepository.findById(groupId);
    }
    
    public List<GroupMember> getGroupMembers(UUID groupId) {
        return groupMemberRepository.findByGroupIdAndIsActiveTrue(groupId);
    }
    
    public GroupMember addMemberToGroup(UUID groupId, UUID userId, GroupRole role) {
        Group group = groupRepository.findById(groupId)
                .orElseThrow(() -> new ResourceNotFoundException("Group", "id", groupId));
        
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new ResourceNotFoundException("User", "id", userId));
        
        GroupMember member = GroupMember.builder()
                .group(group)
                .user(user)
                .role(role)
                .joinedAt(LocalDateTime.now())
                .isActive(true)
                .build();
        
        return groupMemberRepository.save(member);
    }
    
    // FIXED T02: Invite via Email now has proper Admin role check
    public void sendGroupInvite(GroupInviteDto inviteDto, UUID senderId) {
        // Find the group
        Group group = groupRepository.findById(inviteDto.getGroupId())
                .orElseThrow(() -> new ResourceNotFoundException("Group", "id", inviteDto.getGroupId()));
        
        // Find the sender
        User sender = userRepository.findById(senderId)
                .orElseThrow(() -> new ResourceNotFoundException("User", "id", senderId));
        
        // FIXED T02: Check if sender is a member of the group
        Optional<GroupMember> membership = groupMemberRepository.findActiveMembership(group.getId(), senderId);
        if (membership.isEmpty()) {
            throw new AuthorizationException("User is not a member of this group");
        }
        
        // FIXED T02: Check if sender has admin role
        GroupMember member = membership.get();
        if (member.getRole() != GroupRole.ADMIN) {
            throw new AuthorizationException("Only group administrators can send invites");
        }
        
        // FIXED T02: Additional check - verify user is active
        if (!member.getIsActive()) {
            throw new AuthorizationException("User membership is not active");
        }
        
        // All validations passed - send the invite
        System.out.println("Sending invite from " + sender.getUsername() + " to " + inviteDto.getEmail());
    }
    
    public boolean isUserMemberOfGroup(UUID groupId, UUID userId) {
        return groupMemberRepository.existsByGroupIdAndUserIdAndIsActiveTrue(groupId, userId);
    }
    
    public boolean isUserAdminOfGroup(UUID groupId, UUID userId) {
        return groupMemberRepository.existsByGroupIdAndUserIdAndRoleAndIsActiveTrue(groupId, userId, GroupRole.ADMIN);
    }
    
    public void removeMemberFromGroup(UUID groupId, UUID userId) {
        GroupMember member = groupMemberRepository.findByGroupIdAndUserId(groupId, userId)
                .orElseThrow(() -> new ResourceNotFoundException("GroupMember", "groupId and userId", groupId + ", " + userId));
        
        member.setIsActive(false);
        groupMemberRepository.save(member);
    }
    
    public void deactivateGroup(UUID groupId) {
        Group group = groupRepository.findById(groupId)
                .orElseThrow(() -> new ResourceNotFoundException("Group", "id", groupId));
        
        group.setIsActive(false);
        group.setUpdatedAt(LocalDateTime.now());
        groupRepository.save(group);
    }
}
