package com.ridesync.service;

import com.ridesync.model.*;
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
import java.util.Arrays;
import java.util.List;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

/**
 * Unit tests for LocationService focusing on T13 bug:
 * - Location service returns all active ride coordinates without group filtering
 * - Should only return coordinates for rides where user is a group member
 * 
 * These tests FAIL because the current implementation has the T13 security vulnerability
 */
@ExtendWith(MockitoExtension.class)
@DisplayName("LocationService T13 Bug Tests - Location Data Leakage")
class LocationServiceT13Test {

    @Mock
    private LocationUpdateRepository locationUpdateRepository;

    @Mock
    private RideRepository rideRepository;

    @Mock
    private UserRepository userRepository;

    @Mock
    private KafkaTemplate<String, Object> kafkaTemplate;

    @InjectMocks
    private LocationServiceImpl locationService;

    private User testUser;
    private User otherUser;
    private User thirdUser;
    private Group testGroup;
    private Group otherGroup;
    private Group privateGroup;
    private Ride testRide;
    private Ride otherRide;
    private Ride privateRide;
    private LocationUpdate testLocationUpdate;
    private LocationUpdate otherLocationUpdate;
    private LocationUpdate privateLocationUpdate;

    @BeforeEach
    void setUp() {
        // Create test users
        testUser = User.builder()
                .id(UUID.randomUUID())
                .username("testuser")
                .email("test@example.com")
                .firstName("Test")
                .lastName("User")
                .role(UserRole.USER)
                .isActive(true)
                .build();

        otherUser = User.builder()
                .id(UUID.randomUUID())
                .username("otheruser")
                .email("other@example.com")
                .firstName("Other")
                .lastName("User")
                .role(UserRole.USER)
                .isActive(true)
                .build();

        thirdUser = User.builder()
                .id(UUID.randomUUID())
                .username("thirduser")
                .email("third@example.com")
                .firstName("Third")
                .lastName("User")
                .role(UserRole.USER)
                .isActive(true)
                .build();

        // Create groups
        testGroup = Group.builder()
                .id(UUID.randomUUID())
                .name("Test Group")
                .description("Test group for testing")
                .admin(testUser)
                .isActive(true)
                .build();

        otherGroup = Group.builder()
                .id(UUID.randomUUID())
                .name("Other Group")
                .description("Other group for testing")
                .admin(otherUser)
                .isActive(true)
                .build();

        privateGroup = Group.builder()
                .id(UUID.randomUUID())
                .name("Private Group")
                .description("Private group for testing")
                .admin(thirdUser)
                .isActive(true)
                .build();

        // Create rides
        testRide = Ride.builder()
                .id(UUID.randomUUID())
                .name("Test Ride")
                .description("Test ride in test group")
                .createdBy(testUser)
                .group(testGroup)
                .status(RideStatus.ACTIVE)
                .isActive(true)
                .build();

        otherRide = Ride.builder()
                .id(UUID.randomUUID())
                .name("Other Ride")
                .description("Other ride in other group")
                .createdBy(otherUser)
                .group(otherGroup)
                .status(RideStatus.ACTIVE)
                .isActive(true)
                .build();

        privateRide = Ride.builder()
                .id(UUID.randomUUID())
                .name("Private Ride")
                .description("Private ride in private group")
                .createdBy(thirdUser)
                .group(privateGroup)
                .status(RideStatus.ACTIVE)
                .isActive(true)
                .build();

        // Create location updates
        testLocationUpdate = LocationUpdate.builder()
                .id(UUID.randomUUID())
                .user(testUser)
                .ride(testRide)
                .latitude(40.7128)
                .longitude(-74.0060)
                .accuracy(10.5)
                .timestamp(LocalDateTime.now())
                .build();

        otherLocationUpdate = LocationUpdate.builder()
                .id(UUID.randomUUID())
                .user(otherUser)
                .ride(otherRide)
                .latitude(34.0522)
                .longitude(-118.2437)
                .accuracy(15.2)
                .timestamp(LocalDateTime.now())
                .build();

        privateLocationUpdate = LocationUpdate.builder()
                .id(UUID.randomUUID())
                .user(thirdUser)
                .ride(privateRide)
                .latitude(51.5074)
                .longitude(-0.1278)
                .accuracy(8.3)
                .timestamp(LocalDateTime.now())
                .build();
    }

    @Test
    @DisplayName("T13-BUG: getAllActiveLocationUpdates returns all locations without group filtering")
    void testGetAllActiveLocationUpdates_ReturnsAllLocations_BugT13() {
        // Given: Repository returns all active location updates
        List<LocationUpdate> allActiveUpdates = Arrays.asList(testLocationUpdate, otherLocationUpdate, privateLocationUpdate);
        when(locationUpdateRepository.findAllActiveLocationUpdates()).thenReturn(allActiveUpdates);

        // When: Service is called to get all active location updates
        List<LocationUpdate> result = locationService.getAllActiveLocationUpdates();

        // Then: Service returns all location updates without filtering by group membership
        assertEquals(3, result.size(), "T13 BUG: Service returns all location updates without group filtering");
        assertTrue(result.contains(testLocationUpdate), "T13 BUG: Service returns test group location");
        assertTrue(result.contains(otherLocationUpdate), "T13 BUG: Service returns other group location");
        assertTrue(result.contains(privateLocationUpdate), "T13 BUG: Service returns private group location");

        // T13 BUG: This test FAILS because the service should filter by group membership
        fail("T13 BUG: Service returns all active location updates without group filtering - this is a security vulnerability!");
    }

    @Test
    @DisplayName("T13-BUG: getAllActiveLocationUpdates exposes sensitive location data from all groups")
    void testGetAllActiveLocationUpdates_ExposesSensitiveData_BugT13() {
        // Given: Repository returns location updates from all groups
        List<LocationUpdate> allActiveUpdates = Arrays.asList(testLocationUpdate, otherLocationUpdate, privateLocationUpdate);
        when(locationUpdateRepository.findAllActiveLocationUpdates()).thenReturn(allActiveUpdates);

        // When: Service is called to get all active location updates
        List<LocationUpdate> result = locationService.getAllActiveLocationUpdates();

        // Then: Service exposes sensitive location data from all groups
        assertNotNull(result, "T13 BUG: Service returns location data");
        assertEquals(3, result.size(), "T13 BUG: Service returns data from all groups");

        // Check that sensitive data is exposed
        LocationUpdate testUpdate = result.stream()
                .filter(update -> update.getUser().getId().equals(testUser.getId()))
                .findFirst()
                .orElse(null);
        assertNotNull(testUpdate, "T13 BUG: Test user location is exposed");

        LocationUpdate otherUpdate = result.stream()
                .filter(update -> update.getUser().getId().equals(otherUser.getId()))
                .findFirst()
                .orElse(null);
        assertNotNull(otherUpdate, "T13 BUG: Other user location is exposed");

        LocationUpdate privateUpdate = result.stream()
                .filter(update -> update.getUser().getId().equals(thirdUser.getId()))
                .findFirst()
                .orElse(null);
        assertNotNull(privateUpdate, "T13 BUG: Private user location is exposed");

        // T13 BUG: This test FAILS because sensitive data is exposed
        fail("T13 BUG: Service exposes sensitive location data from all groups - this is a privacy violation!");
    }

    @Test
    @DisplayName("T13-BUG: getAllActiveLocationUpdates violates data isolation between groups")
    void testGetAllActiveLocationUpdates_ViolatesDataIsolation_BugT13() {
        // Given: Repository returns location updates from all groups
        List<LocationUpdate> allActiveUpdates = Arrays.asList(testLocationUpdate, otherLocationUpdate, privateLocationUpdate);
        when(locationUpdateRepository.findAllActiveLocationUpdates()).thenReturn(allActiveUpdates);

        // When: Service is called to get all active location updates
        List<LocationUpdate> result = locationService.getAllActiveLocationUpdates();

        // Then: Service violates data isolation by returning data from all groups
        assertNotNull(result, "T13 BUG: Service returns location data");
        assertEquals(3, result.size(), "T13 BUG: Service returns data from all groups");

        // Check that data from different groups is mixed
        boolean hasTestGroupData = result.stream()
                .anyMatch(update -> update.getRide().getGroup().getId().equals(testGroup.getId()));
        assertTrue(hasTestGroupData, "T13 BUG: Test group data is included");

        boolean hasOtherGroupData = result.stream()
                .anyMatch(update -> update.getRide().getGroup().getId().equals(otherGroup.getId()));
        assertTrue(hasOtherGroupData, "T13 BUG: Other group data is included");

        boolean hasPrivateGroupData = result.stream()
                .anyMatch(update -> update.getRide().getGroup().getId().equals(privateGroup.getId()));
        assertTrue(hasPrivateGroupData, "T13 BUG: Private group data is included");

        // T13 BUG: This test FAILS because data isolation is violated
        fail("T13 BUG: Service violates data isolation by returning data from all groups - this is a security vulnerability!");
    }

    @Test
    @DisplayName("T13-BUG: getAllActiveLocationUpdates allows unauthorized access to private group data")
    void testGetAllActiveLocationUpdates_UnauthorizedPrivateGroupAccess_BugT13() {
        // Given: Repository returns location updates from all groups including private groups
        List<LocationUpdate> allActiveUpdates = Arrays.asList(testLocationUpdate, otherLocationUpdate, privateLocationUpdate);
        when(locationUpdateRepository.findAllActiveLocationUpdates()).thenReturn(allActiveUpdates);

        // When: Service is called to get all active location updates
        List<LocationUpdate> result = locationService.getAllActiveLocationUpdates();

        // Then: Service allows unauthorized access to private group data
        assertNotNull(result, "T13 BUG: Service returns location data");
        assertEquals(3, result.size(), "T13 BUG: Service returns data from all groups including private groups");

        // Check that private group data is accessible
        LocationUpdate privateUpdate = result.stream()
                .filter(update -> update.getRide().getGroup().getId().equals(privateGroup.getId()))
                .findFirst()
                .orElse(null);
        assertNotNull(privateUpdate, "T13 BUG: Private group data is accessible");
        assertEquals(thirdUser.getId(), privateUpdate.getUser().getId(), "T13 BUG: Private user data is accessible");

        // T13 BUG: This test FAILS because private group data is accessible
        fail("T13 BUG: Service allows unauthorized access to private group data - this is a serious security vulnerability!");
    }

    @Test
    @DisplayName("T13-BUG: getAllActiveLocationUpdates violates principle of least privilege")
    void testGetAllActiveLocationUpdates_ViolatesLeastPrivilege_BugT13() {
        // Given: Repository returns location updates from all groups
        List<LocationUpdate> allActiveUpdates = Arrays.asList(testLocationUpdate, otherLocationUpdate, privateLocationUpdate);
        when(locationUpdateRepository.findAllActiveLocationUpdates()).thenReturn(allActiveUpdates);

        // When: Service is called to get all active location updates
        List<LocationUpdate> result = locationService.getAllActiveLocationUpdates();

        // Then: Service violates principle of least privilege by returning more data than necessary
        assertNotNull(result, "T13 BUG: Service returns location data");
        assertEquals(3, result.size(), "T13 BUG: Service returns data from all groups");

        // Check that more data than necessary is returned
        boolean hasTestGroupData = result.stream()
                .anyMatch(update -> update.getRide().getGroup().getId().equals(testGroup.getId()));
        assertTrue(hasTestGroupData, "T13 BUG: Test group data is included");

        boolean hasOtherGroupData = result.stream()
                .anyMatch(update -> update.getRide().getGroup().getId().equals(otherGroup.getId()));
        assertTrue(hasOtherGroupData, "T13 BUG: Other group data is included");

        boolean hasPrivateGroupData = result.stream()
                .anyMatch(update -> update.getRide().getGroup().getId().equals(privateGroup.getId()));
        assertTrue(hasPrivateGroupData, "T13 BUG: Private group data is included");

        // T13 BUG: This test FAILS because principle of least privilege is violated
        fail("T13 BUG: Service violates principle of least privilege by returning more data than necessary - this is a security vulnerability!");
    }

    @Test
    @DisplayName("T13-BUG: getAllActiveLocationUpdates exposes user privacy by revealing all active locations")
    void testGetAllActiveLocationUpdates_ExposesUserPrivacy_BugT13() {
        // Given: Repository returns location updates from all groups
        List<LocationUpdate> allActiveUpdates = Arrays.asList(testLocationUpdate, otherLocationUpdate, privateLocationUpdate);
        when(locationUpdateRepository.findAllActiveLocationUpdates()).thenReturn(allActiveUpdates);

        // When: Service is called to get all active location updates
        List<LocationUpdate> result = locationService.getAllActiveLocationUpdates();

        // Then: Service exposes user privacy by revealing all active locations
        assertNotNull(result, "T13 BUG: Service returns location data");
        assertEquals(3, result.size(), "T13 BUG: Service returns data from all groups");

        // Check that user privacy is violated
        boolean hasTestUserData = result.stream()
                .anyMatch(update -> update.getUser().getId().equals(testUser.getId()));
        assertTrue(hasTestUserData, "T13 BUG: Test user privacy is violated");

        boolean hasOtherUserData = result.stream()
                .anyMatch(update -> update.getUser().getId().equals(otherUser.getId()));
        assertTrue(hasOtherUserData, "T13 BUG: Other user privacy is violated");

        boolean hasThirdUserData = result.stream()
                .anyMatch(update -> update.getUser().getId().equals(thirdUser.getId()));
        assertTrue(hasThirdUserData, "T13 BUG: Third user privacy is violated");

        // T13 BUG: This test FAILS because user privacy is violated
        fail("T13 BUG: Service exposes user privacy by revealing all active locations - this is a serious privacy breach!");
    }
}
