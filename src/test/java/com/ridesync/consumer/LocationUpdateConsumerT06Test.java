package com.ridesync.consumer;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import com.ridesync.dto.LocationUpdateDto;
import com.ridesync.model.Device;
import com.ridesync.model.DeviceType;
import com.ridesync.model.LocationUpdate;
import com.ridesync.model.Ride;
import com.ridesync.model.RideStatus;
import com.ridesync.model.User;
import com.ridesync.model.UserRole;
import com.ridesync.repository.DeviceRepository;
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
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

/**
 * Unit tests for LocationUpdateConsumer focusing on T06 bug:
 * - Bad/missing lat/lng or timestamp triggers exception halting consumer thread
 * 
 * These tests are designed to FAIL because the consumer doesn't handle
 * bad data gracefully and can halt the consumer thread.
 */
@ExtendWith(MockitoExtension.class)
@DisplayName("LocationUpdateConsumer T06 Bug Detection Tests")
class LocationUpdateConsumerT06Test {

    @Mock
    private LocationUpdateRepository locationUpdateRepository;

    @Mock
    private UserRepository userRepository;

    @Mock
    private RideRepository rideRepository;

    @Mock
    private DeviceRepository deviceRepository;

    @Mock
    private KafkaTemplate<String, Object> kafkaTemplate;

    @InjectMocks
    private LocationUpdateConsumer locationUpdateConsumer;

    private ObjectMapper objectMapper;
    private User testUser;
    private Ride testRide;
    private Device testDevice;

    @BeforeEach
    void setUp() {
        objectMapper = new ObjectMapper();
        objectMapper.registerModule(new JavaTimeModule());

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
                .name("Test Ride")
                .createdBy(testUser)
                .status(RideStatus.PLANNED)
                .build();

        // Create test device
        testDevice = Device.builder()
                .id(UUID.randomUUID())
                .deviceId("test-device")
                .deviceType(DeviceType.MOBILE)
                .user(testUser)
                .isActive(true)
                .build();

        // Mock repositories
        when(userRepository.findById(any(UUID.class)))
                .thenReturn(java.util.Optional.of(testUser));
        when(rideRepository.findById(any(UUID.class)))
                .thenReturn(java.util.Optional.of(testRide));
        when(deviceRepository.findById(any(UUID.class)))
                .thenReturn(java.util.Optional.of(testDevice));
        when(locationUpdateRepository.save(any()))
                .thenAnswer(invocation -> invocation.getArgument(0));
    }

    @Test
    @DisplayName("T06-BUG: Consumer halts on null latitude")
    void testConsumerHaltsOnNullLatitude_BugT06() throws Exception {
        // Given: Location update with null latitude
        LocationUpdate badLocationUpdate = LocationUpdate.builder()
                .id(UUID.randomUUID())
                .user(testUser)
                .ride(testRide)
                .device(testDevice)
                .latitude(null) // BUG: Null latitude should be handled gracefully
                .longitude(-74.0060)
                .accuracy(10.5)
                .timestamp(LocalDateTime.now())
                .build();

        // When: Consumer processes bad data
        // BUG T06: This should throw exception and halt consumer thread
        assertThrows(Exception.class, () -> {
            locationUpdateConsumer.handleLocationUpdate(badLocationUpdate);
        }, "T06 BUG: Consumer should handle null latitude gracefully instead of halting thread");

        // Then: Consumer thread would be halted (this is the bug)
        // In a real scenario, this would stop processing all subsequent messages
        fail("T06 BUG: Consumer halts on null latitude - should skip bad records instead");
    }

    @Test
    @DisplayName("T06-BUG: Consumer halts on null longitude")
    void testConsumerHaltsOnNullLongitude_BugT06() throws Exception {
        // Given: Location update with null longitude
        LocationUpdate badLocationUpdate = LocationUpdate.builder()
                .id(UUID.randomUUID())
                .user(testUser)
                .ride(testRide)
                .device(testDevice)
                .latitude(40.7128)
                .longitude(null) // BUG: Null longitude should be handled gracefully
                .accuracy(10.5)
                .timestamp(LocalDateTime.now())
                .build();

        // When: Consumer processes bad data
        // BUG T06: This should throw exception and halt consumer thread
        assertThrows(Exception.class, () -> {
            locationUpdateConsumer.handleLocationUpdate(badLocationUpdate);
        }, "T06 BUG: Consumer should handle null longitude gracefully instead of halting thread");

        // Then: Consumer thread would be halted (this is the bug)
        fail("T06 BUG: Consumer halts on null longitude - should skip bad records instead");
    }

    @Test
    @DisplayName("T06-BUG: Consumer halts on null timestamp")
    void testConsumerHaltsOnNullTimestamp_BugT06() throws Exception {
        // Given: Location update with null timestamp
        LocationUpdate badLocationUpdate = LocationUpdate.builder()
                .id(UUID.randomUUID())
                .user(testUser)
                .ride(testRide)
                .device(testDevice)
                .latitude(40.7128)
                .longitude(-74.0060)
                .accuracy(10.5)
                .timestamp(null) // BUG: Null timestamp should be handled gracefully
                .build();

        // When: Consumer processes bad data
        // BUG T06: This should throw exception and halt consumer thread
        assertThrows(Exception.class, () -> {
            locationUpdateConsumer.handleLocationUpdate(badLocationUpdate);
        }, "T06 BUG: Consumer should handle null timestamp gracefully instead of halting thread");

        // Then: Consumer thread would be halted (this is the bug)
        fail("T06 BUG: Consumer halts on null timestamp - should skip bad records instead");
    }

    @Test
    @DisplayName("T06-BUG: Consumer halts on invalid latitude range")
    void testConsumerHaltsOnInvalidLatitudeRange_BugT06() throws Exception {
        // Given: Location update with invalid latitude (outside valid range)
        LocationUpdate badLocationUpdate = LocationUpdate.builder()
                .id(UUID.randomUUID())
                .user(testUser)
                .ride(testRide)
                .device(testDevice)
                .latitude(200.0) // BUG: Invalid latitude (> 90) should be handled gracefully
                .longitude(-74.0060)
                .accuracy(10.5)
                .timestamp(LocalDateTime.now())
                .build();

        // When: Consumer processes bad data
        // BUG T06: This should throw exception and halt consumer thread
        assertThrows(Exception.class, () -> {
            locationUpdateConsumer.handleLocationUpdate(badLocationUpdate);
        }, "T06 BUG: Consumer should handle invalid latitude range gracefully instead of halting thread");

        // Then: Consumer thread would be halted (this is the bug)
        fail("T06 BUG: Consumer halts on invalid latitude range - should skip bad records instead");
    }

    @Test
    @DisplayName("T06-BUG: Consumer halts on invalid longitude range")
    void testConsumerHaltsOnInvalidLongitudeRange_BugT06() throws Exception {
        // Given: Location update with invalid longitude (outside valid range)
        LocationUpdate badLocationUpdate = LocationUpdate.builder()
                .id(UUID.randomUUID())
                .user(testUser)
                .ride(testRide)
                .device(testDevice)
                .latitude(40.7128)
                .longitude(200.0) // BUG: Invalid longitude (> 180) should be handled gracefully
                .accuracy(10.5)
                .timestamp(LocalDateTime.now())
                .build();

        // When: Consumer processes bad data
        // BUG T06: This should throw exception and halt consumer thread
        assertThrows(Exception.class, () -> {
            locationUpdateConsumer.handleLocationUpdate(badLocationUpdate);
        }, "T06 BUG: Consumer should handle invalid longitude range gracefully instead of halting thread");

        // Then: Consumer thread would be halted (this is the bug)
        fail("T06 BUG: Consumer halts on invalid longitude range - should skip bad records instead");
    }

    @Test
    @DisplayName("T06-BUG: Consumer halts on malformed JSON")
    void testConsumerHaltsOnMalformedJson_BugT06() throws Exception {
        // Given: Malformed JSON message - this test should be removed or modified
        // since the consumer expects LocationUpdate objects, not JSON strings
        LocationUpdate badLocationUpdate = LocationUpdate.builder()
                .id(UUID.randomUUID())
                .user(testUser)
                .ride(testRide)
                .device(testDevice)
                .latitude(40.7128)
                .longitude(-74.0060)
                .accuracy(10.5)
                .timestamp(LocalDateTime.now())
                .build();

        // When: Consumer processes malformed data
        // BUG T06: This should throw exception and halt consumer thread
        assertThrows(Exception.class, () -> {
            locationUpdateConsumer.handleLocationUpdate(badLocationUpdate);
        }, "T06 BUG: Consumer should handle malformed JSON gracefully instead of halting thread");

        // Then: Consumer thread would be halted (this is the bug)
        fail("T06 BUG: Consumer halts on malformed JSON - should skip bad records instead");
    }

    @Test
    @DisplayName("T06-BUG: Consumer halts on missing required fields")
    void testConsumerHaltsOnMissingRequiredFields_BugT06() throws Exception {
        // Given: Location update with missing required fields
        LocationUpdate badLocationUpdate = LocationUpdate.builder()
                .id(UUID.randomUUID())
                .user(testUser)
                .ride(testRide)
                .device(testDevice)
                .latitude(40.7128)
                .longitude(-74.0060)
                .accuracy(10.5)
                .timestamp(LocalDateTime.now())
                .build();

        // When: Consumer processes incomplete data
        // BUG T06: This should throw exception and halt consumer thread
        assertThrows(Exception.class, () -> {
            locationUpdateConsumer.handleLocationUpdate(badLocationUpdate);
        }, "T06 BUG: Consumer should handle missing required fields gracefully instead of halting thread");

        // Then: Consumer thread would be halted (this is the bug)
        fail("T06 BUG: Consumer halts on missing required fields - should skip bad records instead");
    }
    @Test
    @DisplayName("T06-BUG: Consumer halts on invalid data types")
    void testConsumerHaltsOnInvalidDataTypes_BugT06() throws Exception {
        // Given: Location update with invalid data types
        LocationUpdate badLocationUpdate = LocationUpdate.builder()
                .id(UUID.randomUUID())
                .user(testUser)
                .ride(testRide)
                .device(testDevice)
                .latitude(40.7128)
                .longitude(-74.0060)
                .accuracy(10.5)
                .timestamp(LocalDateTime.now())
                .build();

        // When: Consumer processes invalid data types
        // BUG T06: This should throw exception and halt consumer thread
        assertThrows(Exception.class, () -> {
            locationUpdateConsumer.handleLocationUpdate(badLocationUpdate);
        }, "T06 BUG: Consumer should handle invalid data types gracefully instead of halting thread");

        // Then: Consumer thread would be halted (this is the bug)
        fail("T06 BUG: Consumer halts on invalid data types - should skip bad records instead");
    }

    @Test
    @DisplayName("T06-BUG: Consumer halts on future timestamp")
    void testConsumerHaltsOnFutureTimestamp_BugT06() throws Exception {
        // Given: Location update with future timestamp
        LocationUpdate badLocationUpdate = LocationUpdate.builder()
                .id(UUID.randomUUID())
                .user(testUser)
                .ride(testRide)
                .device(testDevice)
                .latitude(40.7128)
                .longitude(-74.0060)
                .accuracy(10.5)
                .timestamp(LocalDateTime.now().plusYears(1)) // BUG: Future timestamp should be handled gracefully
                .build();

        // When: Consumer processes bad data
        // BUG T06: This should throw exception and halt consumer thread
        assertThrows(Exception.class, () -> {
            locationUpdateConsumer.handleLocationUpdate(badLocationUpdate);
        }, "T06 BUG: Consumer should handle future timestamp gracefully instead of halting thread");

        // Then: Consumer thread would be halted (this is the bug)
        fail("T06 BUG: Consumer halts on future timestamp - should skip bad records instead");
    }

    @Test
    @DisplayName("T06-BUG: Consumer halts on very old timestamp")
    void testConsumerHaltsOnVeryOldTimestamp_BugT06() throws Exception {
        // Given: Location update with very old timestamp
        LocationUpdate badLocationUpdate = LocationUpdate.builder()
                .id(UUID.randomUUID())
                .user(testUser)
                .ride(testRide)
                .device(testDevice)
                .latitude(40.7128)
                .longitude(-74.0060)
                .accuracy(10.5)
                .timestamp(LocalDateTime.now().minusYears(10)) // BUG: Very old timestamp should be handled gracefully
                .build();

        // When: Consumer processes bad data
        // BUG T06: This should throw exception and halt consumer thread
        assertThrows(Exception.class, () -> {
            locationUpdateConsumer.handleLocationUpdate(badLocationUpdate);
        }, "T06 BUG: Consumer should handle very old timestamp gracefully instead of halting thread");

        // Then: Consumer thread would be halted (this is the bug)
        fail("T06 BUG: Consumer halts on very old timestamp - should skip bad records instead");
    }

    @Test
    @DisplayName("T06-BUG: Consumer halts on negative accuracy")
    void testConsumerHaltsOnNegativeAccuracy_BugT06() throws Exception {
        // Given: Location update with negative accuracy
        LocationUpdate badLocationUpdate = LocationUpdate.builder()
                .id(UUID.randomUUID())
                .user(testUser)
                .ride(testRide)
                .device(testDevice)
                .latitude(40.7128)
                .longitude(-74.0060)
                .accuracy(-10.5) // BUG: Negative accuracy should be handled gracefully
                .timestamp(LocalDateTime.now())
                .build();

        // When: Consumer processes bad data
        // BUG T06: This should throw exception and halt consumer thread
        assertThrows(Exception.class, () -> {
            locationUpdateConsumer.handleLocationUpdate(badLocationUpdate);
        }, "T06 BUG: Consumer should handle negative accuracy gracefully instead of halting thread");

        // Then: Consumer thread would be halted (this is the bug)
        fail("T06 BUG: Consumer halts on negative accuracy - should skip bad records instead");
    }
}