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
    
    public Group createGroup(String name, String description, Long createdBy) {
        Group group = new Group();
        group.setName(name);
        group.setDescription(description);
        group.setCreatedBy(createdBy);
        group.setIsActive(true);
        group.setCreatedAt(LocalDateTime.now());
        
        Group savedGroup = groupRepository.save(group);
        
        // Add creator as admin
        User creator = userRepository.findById(createdBy).orElseThrow();
        GroupMember adminMember = new GroupMember(savedGroup, creator, GroupRole.ADMIN);
        groupMemberRepository.save(adminMember);
        
        return savedGroup;
    }
    
    public List<Group> getUserGroups(Long userId) {
        return groupRepository.findByUserId(userId);
    }
    
    public Optional<Group> findById(Long groupId) {
        return groupRepository.findById(groupId);
    }
    
    public List<GroupMember> getGroupMembers(Long groupId) {
        return groupMemberRepository.findByGroupIdAndIsActiveTrue(groupId);
    }
    
    public GroupMember addMemberToGroup(Long groupId, Long userId, GroupRole role) {
        Group group = groupRepository.findById(groupId).orElseThrow();
        User user = userRepository.findById(userId).orElseThrow();
        
        GroupMember member = new GroupMember(group, user, role);
        return groupMemberRepository.save(member);
    }
    
    // BUG T02: No admin role check for sending invites
    public void sendGroupInvite(GroupInviteDto inviteDto, Long senderId) {
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
    
    public boolean isUserMemberOfGroup(Long userId, Long groupId) {
        return groupMemberRepository.findActiveMembership(groupId, userId).isPresent();
    }
    
    public boolean isUserAdminOfGroup(Long userId, Long groupId) {
        Optional<GroupMember> membership = groupMemberRepository.findActiveMembership(groupId, userId);
        return membership.isPresent() && membership.get().getRole() == GroupRole.ADMIN;
    }
    
    public void removeMemberFromGroup(Long groupId, Long userId) {
        groupMemberRepository.findActiveMembership(groupId, userId).ifPresent(member -> {
            member.setIsActive(false);
            groupMemberRepository.save(member);
        });
    }
    
    public void updateGroup(Group group) {
        groupRepository.save(group);
    }
    
    public void deactivateGroup(Long groupId) {
        groupRepository.findById(groupId).ifPresent(group -> {
            group.setIsActive(false);
            groupRepository.save(group);
        });
    }
}
