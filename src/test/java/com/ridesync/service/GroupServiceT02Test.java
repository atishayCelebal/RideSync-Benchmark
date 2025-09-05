package com.ridesync.service;

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
    @DisplayName("T02-FIXED: Regular member cannot send invites - proper admin check")
    void testRegularMemberCannotSendInvite_Fixed() {
        // Given - regular member tries to send invite
        when(groupRepository.findById(testGroup.getId())).thenReturn(Optional.of(testGroup));
        when(userRepository.findById(regularUser.getId())).thenReturn(Optional.of(regularUser));
        when(groupMemberRepository.findActiveMembership(testGroup.getId(), regularUser.getId()))
                .thenReturn(Optional.of(regularMember)); // Regular member, not admin

        // When & Then
        // FIXED T02: This should now throw AuthorizationException because regular member cannot send invites
        AuthorizationException exception = assertThrows(AuthorizationException.class, () -> {
            groupService.sendGroupInvite(inviteDto, regularUser.getId());
        }, "Regular member should not be able to send invites");

        // Verify the exception message
        assertEquals("Only group administrators can send invites", exception.getMessage());

        // Verify that admin role check was performed
        verify(groupMemberRepository, times(1)).findActiveMembership(testGroup.getId(), regularUser.getId());
        
        // Verify that group and user were found
        verify(groupRepository, times(1)).findById(testGroup.getId());
        verify(userRepository, times(1)).findById(regularUser.getId());
    }

    @Test
    @DisplayName("T02-FIXED: Non-member cannot send invites - proper membership check")
    void testNonMemberCannotSendInvite_Fixed() {
        // Given - non-member tries to send invite
        when(groupRepository.findById(testGroup.getId())).thenReturn(Optional.of(testGroup));
        when(userRepository.findById(nonMemberUser.getId())).thenReturn(Optional.of(nonMemberUser));
        when(groupMemberRepository.findActiveMembership(testGroup.getId(), nonMemberUser.getId()))
                .thenReturn(Optional.empty()); // Not a member

        // When & Then
        // FIXED T02: This should now throw AuthorizationException because non-member cannot send invites
        AuthorizationException exception = assertThrows(AuthorizationException.class, () -> {
            groupService.sendGroupInvite(inviteDto, nonMemberUser.getId());
        }, "Non-member should not be able to send invites");

        // Verify the exception message
        assertEquals("User is not a member of this group", exception.getMessage());

        // Verify that membership check was performed
        verify(groupMemberRepository, times(1)).findActiveMembership(testGroup.getId(), nonMemberUser.getId());
        
        // Verify that group and user were found
        verify(groupRepository, times(1)).findById(testGroup.getId());
        verify(userRepository, times(1)).findById(nonMemberUser.getId());
    }

    @Test
    @DisplayName("T02-FIXED: Anyone cannot send invites - proper authorization check")
    void testAnyoneCannotSendInvite_Fixed() {
        // Given - any user tries to send invite
        when(groupRepository.findById(testGroup.getId())).thenReturn(Optional.of(testGroup));
        when(userRepository.findById(any(UUID.class))).thenReturn(Optional.of(regularUser));
        when(groupMemberRepository.findActiveMembership(eq(testGroup.getId()), any(UUID.class)))
                .thenReturn(Optional.empty()); // Not a member

        // When & Then
        // FIXED T02: This should now throw AuthorizationException because proper authorization check exists
        AuthorizationException exception = assertThrows(AuthorizationException.class, () -> {
            groupService.sendGroupInvite(inviteDto, UUID.randomUUID());
        }, "Should check authorization before allowing invites");

        // Verify the exception message
        assertEquals("User is not a member of this group", exception.getMessage());

        // Verify that proper authorization check was performed
        verify(groupMemberRepository, times(1)).findActiveMembership(eq(testGroup.getId()), any(UUID.class));
        
        // Verify that group and user were found
        verify(groupRepository, times(1)).findById(testGroup.getId());
        verify(userRepository, times(1)).findById(any(UUID.class));
    }

    @Test
    @DisplayName("T02-FIXED: Role validation now works in sendGroupInvite method")
    void testRoleValidationWorks_Fixed() {
        // Given - method now validates admin role
        when(groupRepository.findById(testGroup.getId())).thenReturn(Optional.of(testGroup));
        when(userRepository.findById(regularUser.getId())).thenReturn(Optional.of(regularUser));
        when(groupMemberRepository.findActiveMembership(testGroup.getId(), regularUser.getId()))
                .thenReturn(Optional.of(regularMember)); // Regular member, not admin

        // When & Then
        // FIXED T02: This should now throw AuthorizationException because method validates admin role
        AuthorizationException exception = assertThrows(AuthorizationException.class, () -> {
            groupService.sendGroupInvite(inviteDto, regularUser.getId());
        }, "Method should validate admin role before sending invites");

        // Verify the exception message
        assertEquals("Only group administrators can send invites", exception.getMessage());

        // Verify that admin role validation was performed
        verify(groupMemberRepository, times(1)).findActiveMembership(testGroup.getId(), regularUser.getId());
        
        // Verify that group and user were found
        verify(groupRepository, times(1)).findById(testGroup.getId());
        verify(userRepository, times(1)).findById(regularUser.getId());
    }

    @Test
    @DisplayName("T02-FIXED: Admin role check now prevents unauthorized invites")
    void testAdminRoleCheckPreventsUnauthorizedInvites_Fixed() {
        // Given - system now checks if user is admin
        when(groupRepository.findById(testGroup.getId())).thenReturn(Optional.of(testGroup));
        when(userRepository.findById(regularUser.getId())).thenReturn(Optional.of(regularUser));
        when(groupMemberRepository.findActiveMembership(testGroup.getId(), regularUser.getId()))
                .thenReturn(Optional.of(regularMember)); // Regular member, not admin

        // When & Then
        // FIXED T02: This should now throw AuthorizationException because admin role check exists
        AuthorizationException exception = assertThrows(AuthorizationException.class, () -> {
            groupService.sendGroupInvite(inviteDto, regularUser.getId());
        }, "Admin role check should prevent unauthorized invites");

        // Verify the exception message
        assertEquals("Only group administrators can send invites", exception.getMessage());

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
        when(groupMemberRepository.findActiveMembership(testGroup.getId(), adminUser.getId()))
                .thenReturn(Optional.of(adminMember)); // Admin member

        // When
        // FIXED T02: This should now work because admin has proper permissions
        assertDoesNotThrow(() -> {
            groupService.sendGroupInvite(inviteDto, adminUser.getId());
        }, "Admin should be able to send invites");

        // Verify that group and user were found
        verify(groupRepository, times(1)).findById(testGroup.getId());
        verify(userRepository, times(1)).findById(adminUser.getId());
        
        // Verify that admin role check was performed
        verify(groupMemberRepository, times(1)).findActiveMembership(testGroup.getId(), adminUser.getId());
    }
}
