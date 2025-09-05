package com.ridesync.service;

import com.ridesync.dto.GroupInviteDto;
import com.ridesync.dto.GroupRequestDto;
import com.ridesync.model.Group;
import com.ridesync.model.GroupMember;
import com.ridesync.model.GroupRole;
import com.ridesync.model.User;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface GroupService {
    
    Group createGroup(GroupRequestDto groupRequest, User admin);
    
    List<Group> getUserGroups(UUID userId);
    
    Optional<Group> findById(UUID groupId);
    
    List<GroupMember> getGroupMembers(UUID groupId);
    
    GroupMember addMemberToGroup(UUID groupId, UUID userId, GroupRole role);
    
    void sendGroupInvite(GroupInviteDto inviteDto, UUID senderId);
    
    boolean isUserMemberOfGroup(UUID groupId, UUID userId);
    
    boolean isUserAdminOfGroup(UUID groupId, UUID userId);
    
    void removeMemberFromGroup(UUID groupId, UUID userId);
    
    void deactivateGroup(UUID groupId);
}