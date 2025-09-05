package com.ridesync.service;

import com.ridesync.dto.UserRegistrationDto;
import com.ridesync.exception.DuplicateResourceException;
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
import static org.mockito.Mockito.lenient;

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
    
    @Mock
    private BCryptPasswordEncoder passwordEncoder;

    @InjectMocks
    private UserServiceImpl userService;

    private UserRegistrationDto validRegistrationDto;
    private User existingUser;

    @BeforeEach
    void setUp() {
        // Set up mock behavior for password encoder with lenient stubbing
        lenient().when(passwordEncoder.encode(anyString())).thenAnswer(invocation -> {
            String password = invocation.getArgument(0);
            return "$2a$10$" + password; // Mock BCrypt hash format
        });
        
        lenient().when(passwordEncoder.matches(anyString(), anyString())).thenAnswer(invocation -> {
            String rawPassword = invocation.getArgument(0);
            String hashedPassword = invocation.getArgument(1);
            return hashedPassword.equals("$2a$10$" + rawPassword); // Simple mock logic
        });
        
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
                .password("$2a$10$plaintextpassword") // Mock hashed password
                .firstName("Existing")
                .lastName("User")
                .role(UserRole.USER)
                .isActive(true)
                .build();
    }

    @Test
    @DisplayName("T01-FIXED: Password is now properly hashed")
    void testPasswordHashed_Fixed() {
        // Given
        when(userRepository.save(any(User.class))).thenAnswer(invocation -> {
            User savedUser = invocation.getArgument(0);
            return savedUser;
        });

        // When
        User result = userService.registerUser(validRegistrationDto);

        // Then
        assertNotNull(result);
        
        // Password should be hashed (not equal to original)
        assertNotEquals("plaintextpassword", result.getPassword(), 
            "Password should be hashed, not stored as plaintext");
        
        // Verify password IS hashed using BCrypt
        assertTrue(passwordEncoder.matches("plaintextpassword", result.getPassword()),
            "Password should be properly hashed with BCrypt");
        
        // Verify repository was called
        verify(userRepository, times(1)).save(any(User.class));
    }

    @Test
    @DisplayName("T01-FIXED: Duplicate email is now properly rejected")
    void testDuplicateEmailRejected_Fixed() {
        // Given - user with same email already exists
        when(userRepository.existsByEmail("test@example.com")).thenReturn(true);

        // When & Then
        // Duplicate email should be rejected - this should now work correctly
        assertThrows(DuplicateResourceException.class, () -> {
            userService.registerUser(validRegistrationDto);
        }, "Duplicate email should be rejected");
        
        // Verify that duplicate email check was performed
        verify(userRepository, times(1)).existsByEmail("test@example.com");
        
        // Verify that user was NOT saved due to duplicate email
        verify(userRepository, never()).save(any(User.class));
    }

    @Test
    @DisplayName("T01-FIXED: Password validation now uses BCrypt")
    void testPasswordValidationBCrypt_Fixed() {
        // Given
        String rawPassword = "plaintextpassword";
        String hashedPassword = passwordEncoder.encode(rawPassword);

        // When
        boolean isValid = userService.validatePassword(rawPassword, hashedPassword);

        // Then
        // Password validation should work correctly with BCrypt
        assertTrue(isValid, 
            "Password validation should work correctly with BCrypt");
        
        // Test with wrong password
        boolean isInvalid = userService.validatePassword("wrongpassword", hashedPassword);
        assertFalse(isInvalid, "Wrong password should be rejected");
    }

    @Test
    @DisplayName("T01-FIXED: Registration is now properly rejected for duplicate email")
    void testRegistrationWithExistingEmailRejected_Fixed() {
        // Given - user with same email already exists
        when(userRepository.existsByEmail("test@example.com")).thenReturn(true);

        // When & Then
        // Registration should be rejected for duplicate email - this should now work correctly
        assertThrows(DuplicateResourceException.class, () -> {
            userService.registerUser(validRegistrationDto);
        }, "Registration should be rejected for duplicate email");
        
        // Verify that duplicate email check was performed
        verify(userRepository, times(1)).existsByEmail("test@example.com");
        
        // Verify that user was NOT saved due to duplicate email
        verify(userRepository, never()).save(any(User.class));
    }

    @Test
    @DisplayName("T01-FIXED: Multiple users with same email are now properly rejected")
    void testMultipleUsersSameEmailRejected_Fixed() {
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
        // Second registration should be rejected for duplicate email - this should now work correctly
        assertThrows(DuplicateResourceException.class, () -> {
            userService.registerUser(secondUser);
        }, "Multiple users with same email should not be allowed");
        
        // Verify first user was saved
        assertNotNull(firstUser);
        assertEquals("test@example.com", firstUser.getEmail());
        
        // Verify duplicate email check was performed for second user
        verify(userRepository, times(2)).existsByEmail("test@example.com");
    }
}