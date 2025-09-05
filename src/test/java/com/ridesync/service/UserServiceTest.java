package com.ridesync.service;

import com.ridesync.dto.UserRegistrationDto;
import com.ridesync.model.User;
import com.ridesync.model.UserRole;
import com.ridesync.repository.UserRepository;
import com.ridesync.service.impl.UserServiceImpl;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;

import java.time.LocalDateTime;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.*;

/**
 * Unit tests for UserService focusing on T01 bugs:
 * - No password hashing
 * - Duplicate emails allowed
 */
@ExtendWith(MockitoExtension.class)
@DisplayName("UserService T01 Bug Tests")
class UserServiceTest {

    @Mock
    private UserRepository userRepository;

    @InjectMocks
    private UserServiceImpl userService;

    private UserRegistrationDto validRegistrationDto;
    private User existingUser;
    private BCryptPasswordEncoder passwordEncoder;

    @BeforeEach
    void setUp() {
        passwordEncoder = new BCryptPasswordEncoder();
        
        validRegistrationDto = UserRegistrationDto.builder()
                .username("testuser")
                .email("test@example.com")
                .password("plaintextpassword")
                .firstName("Test")
                .lastName("User")
                .build();

        existingUser = User.builder()
                .id(java.util.UUID.randomUUID())
                .username("existinguser")
                .email("test@example.com") // Same email as registration
                .password("plaintextpassword") // Raw password stored
                .firstName("Existing")
                .lastName("User")
                .role(UserRole.USER)
                .isActive(true)
                .build();
    }

    @Test
    @DisplayName("T01-BUG: Password should be hashed but is stored as plaintext")
    void testPasswordNotHashed_BugT01() {
        // Given
        when(userRepository.save(any(User.class))).thenAnswer(invocation -> {
            User savedUser = invocation.getArgument(0);
            return savedUser;
        });

        // When
        User result = userService.registerUser(validRegistrationDto);

        // Then
        assertNotNull(result);
        
        // BUG T01: This test should FAIL because password is stored as plaintext
        // We expect password to be hashed (not equal to original), but it's stored as plaintext
        assertNotEquals("plaintextpassword", result.getPassword(), 
            "T01 BUG: Password should be hashed but is stored as plaintext - this test should FAIL");
        
        // Verify password IS hashed (this should pass if bug is fixed)
        assertTrue(passwordEncoder.matches("plaintextpassword", result.getPassword()),
            "T01 BUG: Password should be hashed but it's stored as plaintext - this test should FAIL");
        
        // Verify repository was called
        verify(userRepository, times(1)).save(any(User.class));
    }

    @Test
    @DisplayName("T01-BUG: Duplicate email should be rejected but is allowed")
    void testDuplicateEmailAllowed_BugT01() {
        // Given - user with same email already exists
        when(userRepository.existsByEmail("test@example.com")).thenReturn(true);
        when(userRepository.save(any(User.class))).thenAnswer(invocation -> {
            User savedUser = invocation.getArgument(0);
            return savedUser;
        });

        // When & Then
        // BUG T01: This test should FAIL because duplicate email should be rejected
        // We expect an exception to be thrown, but it's not due to the bug
        assertThrows(Exception.class, () -> {
            userService.registerUser(validRegistrationDto);
        }, "T01 BUG: Duplicate email should be rejected but is allowed - this test should FAIL");
        
        // Verify that duplicate email check was performed
        verify(userRepository, times(1)).existsByEmail("test@example.com");
        
        // Verify that user was NOT saved due to duplicate email
        verify(userRepository, never()).save(any(User.class));
    }

    @Test
    @DisplayName("T01-BUG: Password validation uses plaintext comparison instead of BCrypt")
    void testPasswordValidationPlaintext_BugT01() {
        // Given
        String rawPassword = "plaintextpassword";
        String hashedPassword = passwordEncoder.encode(rawPassword);

        // When
        boolean isValid = userService.validatePassword(rawPassword, hashedPassword);

        // Then
        // BUG T01: This test should FAIL because validatePassword uses plaintext comparison
        // We expect BCrypt validation to work, but it uses plaintext comparison
        assertTrue(isValid, 
            "T01 BUG: validatePassword should use BCrypt but uses plaintext comparison - this test should FAIL");
        
        // The correct implementation should return true for valid password
        // But our buggy implementation uses plaintext comparison instead of BCrypt
    }

    @Test
    @DisplayName("T01-BUG: Registration succeeds even with existing email")
    void testRegistrationWithExistingEmail_BugT01() {
        // Given - user with same email already exists
        when(userRepository.existsByEmail("test@example.com")).thenReturn(true);
        when(userRepository.save(any(User.class))).thenAnswer(invocation -> {
            User savedUser = invocation.getArgument(0);
            return savedUser;
        });

        // When & Then
        // BUG T01: This test should FAIL because registration should be rejected for duplicate email
        assertThrows(Exception.class, () -> {
            userService.registerUser(validRegistrationDto);
        }, "T01 BUG: Registration should be rejected for duplicate email - this test should FAIL");
        
        // Verify that duplicate email check was performed
        verify(userRepository, times(1)).existsByEmail("test@example.com");
        
        // Verify that user was NOT saved due to duplicate email
        verify(userRepository, never()).save(any(User.class));
    }

    @Test
    @DisplayName("T01-BUG: Multiple users can have same email")
    void testMultipleUsersSameEmail_BugT01() {
        // Given
        UserRegistrationDto secondUser = UserRegistrationDto.builder()
                .username("anotheruser")
                .email("test@example.com") // Same email
                .password("anotherpassword")
                .firstName("Another")
                .lastName("User")
                .build();

        when(userRepository.existsByEmail("test@example.com")).thenReturn(false).thenReturn(true);
        when(userRepository.save(any(User.class))).thenAnswer(invocation -> {
            User savedUser = invocation.getArgument(0);
            return savedUser;
        });

        // When
        User firstUser = userService.registerUser(validRegistrationDto);
        
        // Then
        // BUG T01: This test should FAIL because second registration should be rejected
        assertThrows(Exception.class, () -> {
            userService.registerUser(secondUser);
        }, "T01 BUG: Multiple users with same email should not be allowed - this test should FAIL");
        
        // Verify first user was saved
        assertNotNull(firstUser);
        assertEquals("test@example.com", firstUser.getEmail());
        
        // Verify duplicate email check was performed for second user
        verify(userRepository, times(2)).existsByEmail("test@example.com");
    }
}