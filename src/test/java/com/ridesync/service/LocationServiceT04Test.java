package com.ridesync.service;

import com.ridesync.dto.LocationUpdateDto;
import com.ridesync.model.Device;
import com.ridesync.model.DeviceType;
import com.ridesync.model.LocationUpdate;
import com.ridesync.model.Ride;
import com.ridesync.model.User;
import com.ridesync.model.UserRole;
import com.ridesync.repository.LocationUpdateRepository;
import com.ridesync.repository.RideRepository;
import com.ridesync.repository.UserRepository;
import com.ridesync.service.impl.LocationServiceImpl;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.kafka.core.KafkaTemplate;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

/**
 * Unit tests for LocationService focusing on T04 bug:
 * - Multiple Active Sessions Per User - No session validation
 * 
 * These tests are designed to FAIL because the system allows
 * multiple active sessions per user per ride.
 */
@ExtendWith(MockitoExtension.class)
@DisplayName("LocationService T04 Bug Detection Tests")
class LocationServiceT04Test {

    @Mock
    private LocationUpdateRepository locationUpdateRepository;

    @Mock
    private UserRepository userRepository;

    @Mock
    private RideRepository rideRepository;

    @Mock
    private KafkaTemplate<String, Object> kafkaTemplate;

    @InjectMocks
    private LocationServiceImpl locationService;

    private User testUser;
    private Ride testRide;
    private Device device1;
    private Device device2;
    private LocationUpdateDto locationDto1;
    private LocationUpdateDto locationDto2;

    @BeforeEach
    void setUp() {
        // Create test user
        testUser = User.builder()
                .id(UUID.randomUUID())
                .username("testuser")
                .email("test@example.com")
                .firstName("Test")
                .lastName("User")
                .role(UserRole.USER)
                .isActive(true)
                .build();

        // Create test ride
        testRide = Ride.builder()
                .id(UUID.randomUUID())
                .build();

        // Create two different devices for same user
        device1 = Device.builder()
                .id(UUID.randomUUID())
                .deviceId("device1")
                .deviceType(DeviceType.MOBILE)
                .user(testUser)
                .isActive(true)
                .build();

        device2 = Device.builder()
                .id(UUID.randomUUID())
                .deviceId("device2")
                .deviceType(DeviceType.MOBILE)
                .user(testUser)
                .isActive(true)
                .build();

        // Create location DTOs for both devices
        locationDto1 = LocationUpdateDto.builder()
                .userId(testUser.getId())
                .rideId(testRide.getId())
                .latitude(40.7128)
                .longitude(-74.0060)
                .accuracy(10.5)
                .timestamp(LocalDateTime.now())
                .deviceId(device1.getId())
                .build();

        locationDto2 = LocationUpdateDto.builder()
                .userId(testUser.getId())
                .rideId(testRide.getId())
                .latitude(40.7589)
                .longitude(-73.9851)
                .accuracy(8.2)
                .timestamp(LocalDateTime.now())
                .deviceId(device2.getId())
                .build();

        // Mock repositories
        when(userRepository.findById(any(UUID.class)))
                .thenReturn(java.util.Optional.of(testUser));
        
        when(rideRepository.findById(any(UUID.class)))
                .thenReturn(java.util.Optional.of(testRide));
        
        when(locationUpdateRepository.save(any(LocationUpdate.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));
        
        lenient().when(locationUpdateRepository.findByRideIdAndUserId(any(UUID.class), any(UUID.class)))
                .thenReturn(List.of());
    }

    @Test
    @DisplayName("T04-BUG: User can create multiple active sessions for same ride")
    void testMultipleActiveSessionsAllowed_BugT04() {
        // Given: User with no active sessions
        // When: User creates first session
        LocationUpdate firstSession = locationService.saveLocationUpdate(locationDto1);
        
        // Then: First session should be created successfully
        assertNotNull(firstSession);
        verify(locationUpdateRepository).save(any(LocationUpdate.class));
        
        // When: User tries to create second session for same ride
        LocationUpdate secondSession = locationService.saveLocationUpdate(locationDto2);
        
        // BUG T04: This should FAIL because user already has active session
        // Currently allows multiple sessions, but should reject second session
        assertNotNull(secondSession); // This will pass (BUG)
        verify(locationUpdateRepository, times(2)).save(any(LocationUpdate.class)); // This will pass (BUG)
        
        // BUG T04: Test should FAIL because system allows multiple sessions
        // Expected: Should throw exception or return null for second session
        // Actual: System allows multiple sessions (BUG)
        fail("T04 BUG: System allows multiple active sessions per user per ride - this should be prevented!");
    }

    @Test
    @DisplayName("T04-BUG: No validation of existing active sessions before creating new ones")
    void testNoSessionValidation_BugT04() {
        // Given: User with existing active session
        LocationUpdate existingSession = LocationUpdate.builder()
                .id(UUID.randomUUID())
                .user(testUser)
                .ride(testRide)
                .device(device1)
                .latitude(40.7128)
                .longitude(-74.0060)
                .timestamp(LocalDateTime.now())
                .build();
        
        lenient().when(locationUpdateRepository.findByRideIdAndUserId(testRide.getId(), testUser.getId()))
                .thenReturn(List.of(existingSession));
        
        // When: User tries to create new session
        LocationUpdate newSession = locationService.saveLocationUpdate(locationDto2);
        
        // BUG T04: This should FAIL because system should validate existing sessions
        // Currently allows new session creation without validation
        assertNotNull(newSession); // This will pass (BUG)
        verify(locationUpdateRepository).save(any(LocationUpdate.class)); // This will pass (BUG)
        
        // BUG T04: Test should FAIL because system doesn't validate existing sessions
        // Expected: Should throw exception or return null when session already exists
        // Actual: System allows new session creation without validation (BUG)
        fail("T04 BUG: System doesn't validate existing active sessions before creating new ones - this should be prevented!");
    }
}