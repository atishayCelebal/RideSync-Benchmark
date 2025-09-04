package com.ridesync.service;

import com.ridesync.dto.GroupInviteDto;
import com.ridesync.model.Group;
import com.ridesync.model.GroupMember;
import com.ridesync.model.GroupRole;
import com.ridesync.model.User;
import com.ridesync.repository.GroupMemberRepository;
import com.ridesync.repository.GroupRepository;
import com.ridesync.repository.UserRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Service
@Transactional
public class GroupService {
    
    @Autowired
    private GroupRepository groupRepository;
    
    @Autowired
    private GroupMemberRepository groupMemberRepository;
    
    @Autowired
    private UserRepository userRepository;
    
    @Autowired
    private JavaMailSender mailSender;
    
    public Group createGroup(String name, String description, User admin) {
        Group group = Group.builder()
                .name(name)
                .description(description)
                .admin(admin)
                .isActive(true)
                .build();
        
        Group savedGroup = groupRepository.save(group);
        
        // Add creator as admin
        GroupMember adminMember = GroupMember.builder()
                .group(savedGroup)
                .user(admin)
                .role(GroupRole.ADMIN)
                .joinedAt(LocalDateTime.now())
                .isActive(true)
                .build();
        groupMemberRepository.save(adminMember);
        
        return savedGroup;
    }
    
    public List<Group> getUserGroups(UUID userId) {
        return groupRepository.findByUserId(userId);
    }
    
    public Optional<Group> findById(UUID groupId) {
        return groupRepository.findById(groupId);
    }
    
    public List<GroupMember> getGroupMembers(UUID groupId) {
        return groupMemberRepository.findByGroupIdAndIsActiveTrue(groupId);
    }
    
    public GroupMember addMemberToGroup(UUID groupId, UUID userId, GroupRole role) {
        Group group = groupRepository.findById(groupId).orElseThrow();
        User user = userRepository.findById(userId).orElseThrow();
        
        GroupMember member = GroupMember.builder()
                .group(group)
                .user(user)
                .role(role)
                .joinedAt(LocalDateTime.now())
                .isActive(true)
                .build();
        return groupMemberRepository.save(member);
    }
    
    // BUG T02: No admin role check for sending invites
    public void sendGroupInvite(GroupInviteDto inviteDto, UUID senderId) {
        // BUG T02: Missing admin role check - anyone can send invites
        Group group = groupRepository.findById(inviteDto.getGroupId()).orElseThrow();
        
        // Send email invite
        SimpleMailMessage message = new SimpleMailMessage();
        message.setTo(inviteDto.getEmail());
        message.setSubject("Invitation to join group: " + group.getName());
        message.setText("You have been invited to join the group: " + group.getName() + 
                       "\n\nMessage: " + inviteDto.getMessage() +
                       "\n\nPlease register and join the group.");
        
        mailSender.send(message);
    }
    
    public boolean isUserMemberOfGroup(UUID userId, UUID groupId) {
        return groupMemberRepository.findActiveMembership(groupId, userId).isPresent();
    }
    
    public boolean isUserAdminOfGroup(UUID userId, UUID groupId) {
        Optional<GroupMember> membership = groupMemberRepository.findActiveMembership(groupId, userId);
        return membership.isPresent() && membership.get().getRole() == GroupRole.ADMIN;
    }
    
    public void removeMemberFromGroup(UUID groupId, UUID userId) {
        groupMemberRepository.findActiveMembership(groupId, userId).ifPresent(member -> {
            member.setIsActive(false);
            groupMemberRepository.save(member);
        });
    }
    
    public void updateGroup(Group group) {
        groupRepository.save(group);
    }
    
    public void deactivateGroup(UUID groupId) {
        groupRepository.findById(groupId).ifPresent(group -> {
            group.setIsActive(false);
            groupRepository.save(group);
        });
    }
}
