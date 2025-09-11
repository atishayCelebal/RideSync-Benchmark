package com.ridesync.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import com.ridesync.dto.LocationUpdateResponseDto;
import com.ridesync.mapper.LocationMapper;
import com.ridesync.model.*;
import com.ridesync.service.GroupService;
import com.ridesync.service.LocationService;
import com.ridesync.service.UserService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.MediaType;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContext;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import java.time.LocalDateTime;
import java.util.Arrays;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

/**
 * Unit tests for LocationController focusing on T13 bug:
 * - Location API returns all active ride coordinates to any logged-in user
 * - Should only return coordinates for rides where user is a group member
 * 
 * These tests FAIL because the current implementation has the T13 security vulnerability
 */
@ExtendWith(MockitoExtension.class)
@DisplayName("LocationController T13 Bug Tests - Location Data Leakage")
class LocationControllerT13Test {

    @Mock
    private LocationService locationService;

    @Mock
    private UserService userService;

    @Mock
    private GroupService groupService;

    @Mock
    private Authentication authentication;

    @Mock
    private SecurityContext securityContext;

    @InjectMocks
    private LocationController locationController;

    private MockMvc mockMvc;
    private ObjectMapper objectMapper;

    private User testUser;
    private User otherUser;
    private Group testGroup;
    private Group otherGroup;
    private Ride testRide;
    private Ride otherRide;
    private LocationUpdate testLocationUpdate;
    private LocationUpdate otherLocationUpdate;

    @BeforeEach
    void setUp() {
        objectMapper = new ObjectMapper();
        objectMapper.registerModule(new JavaTimeModule());

        // Create manual LocationMapper implementation to avoid MapStruct mocking issues
        LocationMapper manualLocationMapper = new LocationMapper() {
            @Override
            public LocationUpdateResponseDto toLocationUpdateResponseDto(LocationUpdate locationUpdate) {
                return LocationUpdateResponseDto.builder()
                        .id(locationUpdate.getId())
                        .userId(locationUpdate.getUser().getId())
                        .rideId(locationUpdate.getRide().getId())
                        .latitude(locationUpdate.getLatitude())
                        .longitude(locationUpdate.getLongitude())
                        .accuracy(locationUpdate.getAccuracy())
                        .timestamp(locationUpdate.getTimestamp())
                        .build();
            }

            @Override
            public List<LocationUpdateResponseDto> toLocationUpdateResponseDtoList(List<LocationUpdate> locationUpdates) {
                return locationUpdates.stream()
                        .map(this::toLocationUpdateResponseDto)
                        .collect(java.util.stream.Collectors.toList());
            }
        };

        // Inject the manual mapper into the controller using reflection
        try {
            java.lang.reflect.Field mapperField = LocationController.class.getDeclaredField("locationMapper");
            mapperField.setAccessible(true);
            mapperField.set(locationController, manualLocationMapper);
        } catch (Exception e) {
            throw new RuntimeException("Failed to inject LocationMapper", e);
        }

        mockMvc = MockMvcBuilders.standaloneSetup(locationController).build();

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

        // Create other user
        otherUser = User.builder()
                .id(UUID.randomUUID())
                .username("otheruser")
                .email("other@example.com")
                .firstName("Other")
                .lastName("User")
                .role(UserRole.USER)
                .isActive(true)
                .build();

        // Create test group
        testGroup = Group.builder()
                .id(UUID.randomUUID())
                .name("Test Group")
                .description("Test group for testing")
                .admin(testUser)
                .isActive(true)
                .build();

        // Create other group
        otherGroup = Group.builder()
                .id(UUID.randomUUID())
                .name("Other Group")
                .description("Other group for testing")
                .admin(otherUser)
                .isActive(true)
                .build();

        // Create test ride
        testRide = Ride.builder()
                .id(UUID.randomUUID())
                .name("Test Ride")
                .description("Test ride in test group")
                .createdBy(testUser)
                .group(testGroup)
                .status(RideStatus.ACTIVE)
                .isActive(true)
                .build();

        // Create other ride
        otherRide = Ride.builder()
                .id(UUID.randomUUID())
                .name("Other Ride")
                .description("Other ride in other group")
                .createdBy(otherUser)
                .group(otherGroup)
                .status(RideStatus.ACTIVE)
                .isActive(true)
                .build();

        // Create test location update
        testLocationUpdate = LocationUpdate.builder()
                .id(UUID.randomUUID())
                .user(testUser)
                .ride(testRide)
                .latitude(40.7128)
                .longitude(-74.0060)
                .accuracy(10.5)
                .timestamp(LocalDateTime.now())
                .build();

        // Create other location update
        otherLocationUpdate = LocationUpdate.builder()
                .id(UUID.randomUUID())
                .user(otherUser)
                .ride(otherRide)
                .latitude(34.0522)
                .longitude(-118.2437)
                .accuracy(15.2)
                .timestamp(LocalDateTime.now())
                .build();

        // Note: Response DTOs are created by the manual LocationMapper implementation

        // Setup security context
        when(securityContext.getAuthentication()).thenReturn(authentication);
        SecurityContextHolder.setContext(securityContext);
        when(authentication.isAuthenticated()).thenReturn(true);
        when(authentication.getName()).thenReturn("testuser");
    }

    @Test
    @DisplayName("T13-BUG: User can access all active location updates regardless of group membership")
    void testGetAllActiveLocationUpdates_ReturnsAllLocations_BugT13() throws Exception {
        // Given: User is authenticated but NOT a member of other group
        when(userService.findByUsername("testuser")).thenReturn(Optional.of(testUser));
        
        // T13 BUG: Service returns ALL active location updates, not just user's group
        List<LocationUpdate> allActiveUpdates = Arrays.asList(testLocationUpdate, otherLocationUpdate);
        when(locationService.getAllActiveLocationUpdates()).thenReturn(allActiveUpdates);
        
        // T13 BUG: Mapper converts all updates, including those from other groups
        // Note: LocationMapper is manually implemented, so no mocking needed

        // When: User requests all active location updates
        mockMvc.perform(get("/api/v1/location/active")
                .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data").isArray())
                .andExpect(jsonPath("$.data.length()").value(2))
                .andExpect(jsonPath("$.data[0].userId").value(testUser.getId().toString()))
                .andExpect(jsonPath("$.data[1].userId").value(otherUser.getId().toString()));

        // Then: Service should be called to get all active updates
        verify(locationService).getAllActiveLocationUpdates();
        
        // T13 BUG: This test FAILS because user can see other group's location data
        // The user should only see location updates from groups they are a member of
        fail("T13 BUG: User can access location data from groups they are not a member of - this is a security vulnerability!");
    }

    @Test
    @DisplayName("T13-BUG: User can access location data from private groups they don't belong to")
    void testGetAllActiveLocationUpdates_AccessesPrivateGroupData_BugT13() throws Exception {
        // Given: User is authenticated and member of test group only
        when(userService.findByUsername("testuser")).thenReturn(Optional.of(testUser));
        when(groupService.isUserMemberOfGroup(testGroup.getId(), testUser.getId())).thenReturn(true);
        when(groupService.isUserMemberOfGroup(otherGroup.getId(), testUser.getId())).thenReturn(false);
        
        // T13 BUG: Service returns location updates from ALL groups, including private ones
        List<LocationUpdate> allActiveUpdates = Arrays.asList(testLocationUpdate, otherLocationUpdate);
        when(locationService.getAllActiveLocationUpdates()).thenReturn(allActiveUpdates);
        
        // Note: LocationMapper is manually implemented, so no mocking needed

        // When: User requests all active location updates
        mockMvc.perform(get("/api/v1/location/active")
                .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.length()").value(2));

        // Then: User should NOT be able to see other group's private location data
        // T13 BUG: This test FAILS because the API doesn't filter by group membership
        fail("T13 BUG: User can access private group location data without being a member - this is a security vulnerability!");
    }

    @Test
    @DisplayName("T13-BUG: Location API exposes sensitive location data to unauthorized users")
    void testGetAllActiveLocationUpdates_ExposesSensitiveData_BugT13() throws Exception {
        // Given: User is authenticated but should only see their own group's data
        when(userService.findByUsername("testuser")).thenReturn(Optional.of(testUser));
        
        // T13 BUG: Service returns sensitive location data from all users
        List<LocationUpdate> allActiveUpdates = Arrays.asList(testLocationUpdate, otherLocationUpdate);
        when(locationService.getAllActiveLocationUpdates()).thenReturn(allActiveUpdates);
        
        // Note: LocationMapper is manually implemented, so no mocking needed

        // When: User requests all active location updates
        mockMvc.perform(get("/api/v1/location/active")
                .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data[0].latitude").value(40.7128))
                .andExpect(jsonPath("$.data[0].longitude").value(-74.0060))
                .andExpect(jsonPath("$.data[1].latitude").value(34.0522))
                .andExpect(jsonPath("$.data[1].longitude").value(-118.2437));

        // Then: User should only see location data from their own groups
        // T13 BUG: This test FAILS because sensitive location data is exposed
        fail("T13 BUG: Sensitive location data is exposed to users who should not have access - this is a privacy violation!");
    }

    @Test
    @DisplayName("T13-BUG: No group membership validation in location API")
    void testGetAllActiveLocationUpdates_NoGroupValidation_BugT13() throws Exception {
        // Given: User is authenticated
        when(userService.findByUsername("testuser")).thenReturn(Optional.of(testUser));
        
        // T13 BUG: Service returns all updates without checking group membership
        List<LocationUpdate> allActiveUpdates = Arrays.asList(testLocationUpdate, otherLocationUpdate);
        when(locationService.getAllActiveLocationUpdates()).thenReturn(allActiveUpdates);
        
        // Note: LocationMapper is manually implemented, so no mocking needed

        // When: User requests all active location updates
        mockMvc.perform(get("/api/v1/location/active")
                .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk());

        // Then: GroupService should be called to validate group membership, but it's not
        verify(groupService, never()).isUserMemberOfGroup(any(), any());
        
        // T13 BUG: This test FAILS because there's no group membership validation
        fail("T13 BUG: No group membership validation in location API - this allows unauthorized access to location data!");
    }

    @Test
    @DisplayName("T13-BUG: Location API allows data leakage across different organizations")
    void testGetAllActiveLocationUpdates_CrossOrganizationLeakage_BugT13() throws Exception {
        // Given: User from one organization can see data from another organization
        when(userService.findByUsername("testuser")).thenReturn(Optional.of(testUser));
        
        // T13 BUG: Service returns location updates from all organizations
        List<LocationUpdate> allActiveUpdates = Arrays.asList(testLocationUpdate, otherLocationUpdate);
        when(locationService.getAllActiveLocationUpdates()).thenReturn(allActiveUpdates);
        
        // Note: LocationMapper is manually implemented, so no mocking needed

        // When: User requests all active location updates
        mockMvc.perform(get("/api/v1/location/active")
                .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.length()").value(2));

        // Then: User should only see data from their own organization/groups
        // T13 BUG: This test FAILS because data leaks across organizational boundaries
        fail("T13 BUG: Location data leaks across organizational boundaries - this is a serious security vulnerability!");
    }

    @Test
    @DisplayName("T13-BUG: Location API violates principle of least privilege")
    void testGetAllActiveLocationUpdates_ViolatesLeastPrivilege_BugT13() throws Exception {
        // Given: User should only have access to their own group's data
        when(userService.findByUsername("testuser")).thenReturn(Optional.of(testUser));
        
        // T13 BUG: Service grants access to more data than necessary
        List<LocationUpdate> allActiveUpdates = Arrays.asList(testLocationUpdate, otherLocationUpdate);
        when(locationService.getAllActiveLocationUpdates()).thenReturn(allActiveUpdates);
        
        // Note: LocationMapper is manually implemented, so no mocking needed

        // When: User requests all active location updates
        mockMvc.perform(get("/api/v1/location/active")
                .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk());

        // Then: User should only have access to their own group's data
        // T13 BUG: This test FAILS because the principle of least privilege is violated
        fail("T13 BUG: Location API violates principle of least privilege - users have access to more data than they need!");
    }
}
