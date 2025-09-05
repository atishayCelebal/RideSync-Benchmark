package com.ridesync.service;

import com.ridesync.dto.GroupInviteDto;
import com.ridesync.exception.ResourceNotFoundException;
import com.ridesync.model.Group;
import com.ridesync.model.GroupMember;
import com.ridesync.model.GroupRole;
import com.ridesync.model.User;
import com.ridesync.repository.GroupMemberRepository;
import com.ridesync.repository.GroupRepository;
import com.ridesync.repository.UserRepository;
import com.ridesync.service.impl.GroupServiceImpl;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.mail.javamail.JavaMailSender;

import java.time.LocalDateTime;
import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

/**
 * Unit tests for GroupService focusing on T02 bug:
 * - Invite via Email lacks Admin role check – Anyone can send invites
 */
@ExtendWith(MockitoExtension.class)
@DisplayName("GroupService T02 Bug Tests")
class GroupServiceT02Test {

    @Mock
    private GroupRepository groupRepository;
    
    @Mock
    private GroupMemberRepository groupMemberRepository;
    
    @Mock
    private UserRepository userRepository;
    
    @Mock
    private JavaMailSender mailSender;

    @InjectMocks
    private GroupServiceImpl groupService;

    private Group testGroup;
    private User adminUser;
    private User regularUser;
    private User nonMemberUser;
    private GroupInviteDto inviteDto;
    private GroupMember adminMember;
    private GroupMember regularMember;

    @BeforeEach
    void setUp() {
        // Create test group
        testGroup = Group.builder()
                .id(UUID.randomUUID())
                .name("Test Group")
                .description("Test Description")
                .isActive(true)
                .build();

        // Create admin user
        adminUser = User.builder()
                .id(UUID.randomUUID())
                .username("admin")
                .email("admin@example.com")
                .firstName("Admin")
                .lastName("User")
                .build();

        // Create regular user
        regularUser = User.builder()
                .id(UUID.randomUUID())
                .username("regular")
                .email("regular@example.com")
                .firstName("Regular")
                .lastName("User")
                .build();

        // Create non-member user
        nonMemberUser = User.builder()
                .id(UUID.randomUUID())
                .username("outsider")
                .email("outsider@example.com")
                .firstName("Outsider")
                .lastName("User")
                .build();

        // Create invite DTO
        inviteDto = GroupInviteDto.builder()
                .groupId(testGroup.getId())
                .email("invite@example.com")
                .message("Join our group!")
                .build();

        // Create admin member
        adminMember = GroupMember.builder()
                .id(UUID.randomUUID())
                .group(testGroup)
                .user(adminUser)
                .role(GroupRole.ADMIN)
                .joinedAt(LocalDateTime.now())
                .isActive(true)
                .build();

        // Create regular member
        regularMember = GroupMember.builder()
                .id(UUID.randomUUID())
                .group(testGroup)
                .user(regularUser)
                .role(GroupRole.MEMBER)
                .joinedAt(LocalDateTime.now())
                .isActive(true)
                .build();
    }

    @Test
    @DisplayName("T02-BUG: Regular member can send invites without admin check")
    void testRegularMemberCanSendInvite_BugT02() {
        // Given - regular member tries to send invite
        when(groupRepository.findById(testGroup.getId())).thenReturn(Optional.of(testGroup));
        when(userRepository.findById(regularUser.getId())).thenReturn(Optional.of(regularUser));
        when(groupMemberRepository.findActiveMembership(testGroup.getId(), regularUser.getId()))
                .thenReturn(Optional.of(regularMember)); // Regular member, not admin

        // When & Then
        // BUG T02: This test should FAIL because regular member should not be able to send invites
        assertThrows(Exception.class, () -> {
            groupService.sendGroupInvite(inviteDto, regularUser.getId());
        }, "T02 BUG: Regular member should not be able to send invites - this test should FAIL");

        // Verify that admin role check was performed
        verify(groupMemberRepository, times(1)).findActiveMembership(testGroup.getId(), regularUser.getId());
        
        // Verify that group and user were found
        verify(groupRepository, times(1)).findById(testGroup.getId());
        verify(userRepository, times(1)).findById(regularUser.getId());
    }

    @Test
    @DisplayName("T02-BUG: Non-member can send invites without admin check")
    void testNonMemberCanSendInvite_BugT02() {
        // Given - non-member tries to send invite
        when(groupRepository.findById(testGroup.getId())).thenReturn(Optional.of(testGroup));
        when(userRepository.findById(nonMemberUser.getId())).thenReturn(Optional.of(nonMemberUser));
        when(groupMemberRepository.findActiveMembership(testGroup.getId(), nonMemberUser.getId()))
                .thenReturn(Optional.empty()); // Not a member

        // When & Then
        // BUG T02: This test should FAIL because non-member should not be able to send invites
        assertThrows(Exception.class, () -> {
            groupService.sendGroupInvite(inviteDto, nonMemberUser.getId());
        }, "T02 BUG: Non-member should not be able to send invites - this test should FAIL");

        // Verify that admin role check was performed
        verify(groupMemberRepository, times(1)).findActiveMembership(testGroup.getId(), nonMemberUser.getId());
        
        // Verify that group and user were found
        verify(groupRepository, times(1)).findById(testGroup.getId());
        verify(userRepository, times(1)).findById(nonMemberUser.getId());
    }

    @Test
    @DisplayName("T02-BUG: Anyone can send invites without proper authorization")
    void testAnyoneCanSendInvite_BugT02() {
        // Given - any user tries to send invite
        when(groupRepository.findById(testGroup.getId())).thenReturn(Optional.of(testGroup));
        when(userRepository.findById(any(UUID.class))).thenReturn(Optional.of(regularUser));
        when(groupMemberRepository.findActiveMembership(eq(testGroup.getId()), any(UUID.class)))
                .thenReturn(Optional.empty()); // Not a member

        // When & Then
        // BUG T02: This test should FAIL because there should be proper authorization check
        assertThrows(Exception.class, () -> {
            groupService.sendGroupInvite(inviteDto, UUID.randomUUID());
        }, "T02 BUG: Should check authorization before allowing invites - this test should FAIL");

        // Verify that proper authorization check was performed
        verify(groupMemberRepository, times(1)).findActiveMembership(eq(testGroup.getId()), any(UUID.class));
        
        // Verify that group and user were found
        verify(groupRepository, times(1)).findById(testGroup.getId());
        verify(userRepository, times(1)).findById(any(UUID.class));
    }

    @Test
    @DisplayName("T02-BUG: No role validation in sendGroupInvite method")
    void testNoRoleValidation_BugT02() {
        // Given - method should validate admin role but doesn't
        when(groupRepository.findById(testGroup.getId())).thenReturn(Optional.of(testGroup));
        when(userRepository.findById(regularUser.getId())).thenReturn(Optional.of(regularUser));
        when(groupMemberRepository.findActiveMembership(testGroup.getId(), regularUser.getId()))
                .thenReturn(Optional.of(regularMember)); // Regular member, not admin

        // When & Then
        // BUG T02: This test should FAIL because method should validate admin role
        assertThrows(Exception.class, () -> {
            groupService.sendGroupInvite(inviteDto, regularUser.getId());
        }, "T02 BUG: Method should validate admin role before sending invites - this test should FAIL");

        // Verify that admin role validation was performed
        verify(groupMemberRepository, times(1)).findActiveMembership(testGroup.getId(), regularUser.getId());
        
        // Verify that group and user were found
        verify(groupRepository, times(1)).findById(testGroup.getId());
        verify(userRepository, times(1)).findById(regularUser.getId());
    }

    @Test
    @DisplayName("T02-BUG: Missing admin role check allows unauthorized invites")
    void testMissingAdminRoleCheck_BugT02() {
        // Given - system should check if user is admin but doesn't
        when(groupRepository.findById(testGroup.getId())).thenReturn(Optional.of(testGroup));
        when(userRepository.findById(regularUser.getId())).thenReturn(Optional.of(regularUser));
        when(groupMemberRepository.findActiveMembership(testGroup.getId(), regularUser.getId()))
                .thenReturn(Optional.of(regularMember)); // Regular member, not admin

        // When & Then
        // BUG T02: This test should FAIL because admin role check is missing
        assertThrows(Exception.class, () -> {
            groupService.sendGroupInvite(inviteDto, regularUser.getId());
        }, "T02 BUG: Missing admin role check allows unauthorized invites - this test should FAIL");

        // Verify that admin role was checked
        verify(groupMemberRepository, times(1)).findActiveMembership(testGroup.getId(), regularUser.getId());
        
        // Verify that group and user were found
        verify(groupRepository, times(1)).findById(testGroup.getId());
        verify(userRepository, times(1)).findById(regularUser.getId());
    }

    @Test
    @DisplayName("T02-FIXED: Only admin should be able to send invites")
    void testOnlyAdminCanSendInvite_Fixed() {
        // Given - admin user tries to send invite
        when(groupRepository.findById(testGroup.getId())).thenReturn(Optional.of(testGroup));
        when(userRepository.findById(adminUser.getId())).thenReturn(Optional.of(adminUser));

        // When
        // This test shows what SHOULD happen when the bug is fixed
        // Currently it will pass because the bug allows anyone to send invites
        assertDoesNotThrow(() -> {
            groupService.sendGroupInvite(inviteDto, adminUser.getId());
        }, "Currently admin can send invites (due to bug), but this should be properly validated");

        // Verify that group and user were found
        verify(groupRepository, times(1)).findById(testGroup.getId());
        verify(userRepository, times(1)).findById(adminUser.getId());
        
        // Note: When bug is fixed, this should also verify admin role check
        // verify(groupMemberRepository, times(1)).findActiveMembership(testGroup.getId(), adminUser.getId());
    }
}
