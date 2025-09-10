package com.ridesync.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import com.ridesync.dto.LocationUpdateDto;
import com.ridesync.dto.LocationUpdateResponseDto;
import com.ridesync.mapper.LocationMapper;
import com.ridesync.model.Device;
import com.ridesync.model.DeviceType;
import com.ridesync.model.LocationUpdate;
import com.ridesync.model.User;
import com.ridesync.model.UserRole;
import com.ridesync.service.LocationService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.lenient;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

/**
 * Fixed unit tests for LocationController focusing on T03 bug:
 * - Insecure Location API – No authentication on GPS endpoint
 * 
 * These tests are designed to FAIL because they expect authentication
 * but the current implementation allows unauthorized access.
 */
@ExtendWith(MockitoExtension.class)
@DisplayName("LocationController T03 Bug Fixed Tests")
class LocationControllerT03FixedTest {

    private MockMvc mockMvc;

    @Mock
    private LocationService locationService;

    @InjectMocks
    private LocationController locationController;

    private ObjectMapper objectMapper;
    private LocationUpdateDto validLocationDto;
    private User mockUser;
    private Device mockDevice;
    private LocationUpdate mockLocationUpdate;

    @BeforeEach
    void setUp() {
        // Configure ObjectMapper to handle LocalDateTime
        objectMapper = new ObjectMapper();
        objectMapper.registerModule(new JavaTimeModule());

        // Create mock user
        mockUser = User.builder()
                .id(UUID.randomUUID())
                .username("testuser")
                .email("test@example.com")
                .firstName("Test")
                .lastName("User")
                .role(UserRole.USER)
                .isActive(true)
                .build();

        // Create mock device
        mockDevice = Device.builder()
                .id(UUID.randomUUID())
                .deviceId("device123")
                .deviceType(DeviceType.MOBILE)
                .user(mockUser)
                .isActive(true)
                .build();

        // Create mock location update
        mockLocationUpdate = LocationUpdate.builder()
                .id(UUID.randomUUID())
                .user(mockUser)
                .latitude(40.7128)
                .longitude(-74.0060)
                .accuracy(10.5)
                .timestamp(LocalDateTime.now())
                .device(mockDevice)
                .build();

        // Create valid location DTO
        validLocationDto = LocationUpdateDto.builder()
                .userId(mockUser.getId())
                .rideId(UUID.randomUUID())
                .latitude(40.7128)
                .longitude(-74.0060)
                .accuracy(10.5)
                .timestamp(LocalDateTime.now())
                .deviceId(mockDevice.getId())
                .build();

        // Mock the locationService to return a valid response
        lenient().when(locationService.saveLocationUpdate(any(LocationUpdateDto.class)))
                .thenReturn(mockLocationUpdate);

        lenient().when(locationService.getLocationUpdatesForUser(any(UUID.class)))
                .thenReturn(List.of(mockLocationUpdate));

        lenient().when(locationService.getAllActiveLocationUpdates())
                .thenReturn(List.of(mockLocationUpdate));

        lenient().when(locationService.getLocationUpdatesForRide(any(UUID.class)))
                .thenReturn(List.of(mockLocationUpdate));

        // Create a manual LocationMapper implementation to avoid MapStruct mocking issues
        LocationMapper locationMapper = new LocationMapper() {
            @Override
            public LocationUpdateResponseDto toLocationUpdateResponseDto(LocationUpdate locationUpdate) {
                return LocationUpdateResponseDto.builder()
                        .id(locationUpdate.getId())
                        .userId(locationUpdate.getUser().getId())
                        .rideId(UUID.randomUUID()) // Mock ride ID
                        .latitude(locationUpdate.getLatitude())
                        .longitude(locationUpdate.getLongitude())
                        .accuracy(locationUpdate.getAccuracy())
                        .timestamp(locationUpdate.getTimestamp())
                        .isActive(true)
                        .build();
            }

            @Override
            public List<LocationUpdateResponseDto> toLocationUpdateResponseDtoList(List<LocationUpdate> locationUpdates) {
                return locationUpdates.stream()
                        .map(this::toLocationUpdateResponseDto)
                        .collect(Collectors.toList());
            }
        };

        // Use reflection to inject the manual mapper
        try {
            java.lang.reflect.Field mapperField = LocationController.class.getDeclaredField("locationMapper");
            mapperField.setAccessible(true);
            mapperField.set(locationController, locationMapper);
        } catch (Exception e) {
            throw new RuntimeException("Failed to inject LocationMapper", e);
        }

        mockMvc = MockMvcBuilders.standaloneSetup(locationController).build();
    }

    @Test
    @DisplayName("T03-BUG: Location update endpoint is accessible without authentication")
    void testLocationUpdateAccessibleWithoutAuth_BugT03() throws Exception {
        // Given - No authentication
        String locationJson = objectMapper.writeValueAsString(validLocationDto);

        // When & Then
        // BUG T03: This test should FAIL because endpoint should require authentication
        mockMvc.perform(post("/api/v1/location/update")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(locationJson))
                .andExpect(status().isUnauthorized()) // This should fail - currently returns 200 OK
                .andExpect(jsonPath("$.success").value(false))
                .andExpect(jsonPath("$.error").value("Authentication required"));
    }

    @Test
    @DisplayName("T03-BUG: Location data is accessible without authentication")
    void testLocationDataAccessibleWithoutAuth_BugT03() throws Exception {
        // Given - No authentication
        UUID rideId = UUID.randomUUID();

        // When & Then
        // BUG T03: This test should FAIL because endpoint should require authentication
        mockMvc.perform(get("/api/v1/location/ride/{rideId}", rideId))
                .andExpect(status().isUnauthorized()) // This should fail - currently returns 200 OK
                .andExpect(jsonPath("$.success").value(false))
                .andExpect(jsonPath("$.error").value("Authentication required"));
    }

    @Test
    @DisplayName("T03-BUG: Anyone can inject fake GPS data without authentication")
    void testAnyoneCanInjectFakeGpsData_BugT03() throws Exception {
        // Given - Malicious user with fake GPS data
        LocationUpdateDto fakeLocationDto = LocationUpdateDto.builder()
                .userId(UUID.randomUUID()) // Fake user ID
                .rideId(UUID.randomUUID())
                .latitude(0.0) // Fake coordinates
                .longitude(0.0)
                .accuracy(1000.0) // Very inaccurate
                .timestamp(LocalDateTime.now())
                .deviceId(UUID.randomUUID())
                .build();

        String locationJson = objectMapper.writeValueAsString(fakeLocationDto);

        // When & Then
        // BUG T03: This test should FAIL because endpoint should require authentication
        mockMvc.perform(post("/api/v1/location/update")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(locationJson))
                .andExpect(status().isUnauthorized()) // This should fail - currently returns 200 OK
                .andExpect(jsonPath("$.success").value(false))
                .andExpect(jsonPath("$.error").value("Authentication required"));
    }
}