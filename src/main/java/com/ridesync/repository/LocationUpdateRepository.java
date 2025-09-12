package com.ridesync.repository;

import com.ridesync.model.LocationUpdate;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

@Repository
public interface LocationUpdateRepository extends JpaRepository<LocationUpdate, UUID> {
    
    List<LocationUpdate> findByRideIdOrderByTimestampDesc(UUID rideId);
    
    List<LocationUpdate> findByUserIdOrderByTimestampDesc(UUID userId);
    
    @Query("SELECT lu FROM LocationUpdate lu WHERE lu.ride.id = :rideId AND lu.timestamp >= :since ORDER BY lu.timestamp DESC")
    List<LocationUpdate> findByRideIdAndTimestampAfter(@Param("rideId") UUID rideId, @Param("since") LocalDateTime since);
    
    @Query("SELECT lu FROM LocationUpdate lu WHERE lu.ride.id = :rideId AND lu.user.id = :userId ORDER BY lu.timestamp DESC")
    List<LocationUpdate> findByRideIdAndUserId(@Param("rideId") UUID rideId, @Param("userId") UUID userId);
    
    // BUG T13: Unrestricted query - no group filtering
    @Query("SELECT lu FROM LocationUpdate lu WHERE lu.ride.status = 'ACTIVE' ORDER BY lu.timestamp DESC")
    List<LocationUpdate> findAllActiveLocationUpdates();
    
    // FIXED T13: Group-filtered query - only returns location updates from user's groups
    @Query("SELECT lu FROM LocationUpdate lu " +
           "JOIN lu.ride r " +
           "JOIN r.group g " +
           "JOIN g.members gm " +
           "WHERE gm.user.id = :userId " +
           "AND gm.isActive = true " +
           "AND r.status = 'ACTIVE' " +
           "ORDER BY lu.timestamp DESC")
    List<LocationUpdate> findActiveLocationUpdatesByUserGroups(@Param("userId") UUID userId);
    
    @Query("SELECT lu FROM LocationUpdate lu WHERE lu.ride.id = :rideId AND lu.device.id = :deviceId ORDER BY lu.timestamp DESC")
    List<LocationUpdate> findByRideIdAndDeviceId(@Param("rideId") UUID rideId, @Param("deviceId") UUID deviceId);
    
    // Group-based location queries
    @Query("SELECT lu FROM LocationUpdate lu " +
           "JOIN lu.ride r " +
           "JOIN r.group g " +
           "WHERE g.id = :groupId AND r.id = :rideId " +
           "ORDER BY lu.timestamp DESC")
    List<LocationUpdate> findByGroupIdAndRideIdOrderByTimestampDesc(@Param("groupId") UUID groupId, @Param("rideId") UUID rideId);
    
    @Query("SELECT lu FROM LocationUpdate lu " +
           "JOIN lu.ride r " +
           "JOIN r.group g " +
           "WHERE g.id = :groupId AND r.status = 'ACTIVE' " +
           "AND lu.timestamp = (SELECT MAX(lu2.timestamp) FROM LocationUpdate lu2 " +
           "WHERE lu2.user.id = lu.user.id AND lu2.ride.id = r.id) " +
           "ORDER BY lu.timestamp DESC")
    List<LocationUpdate> findCurrentGroupLocations(@Param("groupId") UUID groupId);
    
    // BUG T16: Inefficient nearby query - no bounding box filter
    @Query(value = """
    SELECT * FROM location_updates lu 
    JOIN rides r ON lu.ride_id = r.id 
    WHERE ST_DWithin(
        ST_GeogFromText(CONCAT('POINT(', lu.longitude, ' ', lu.latitude, ')')), 
        ST_GeogFromText(CONCAT('POINT(', :longitude, ' ', :latitude, ')')), 
        :radius
    ) 
    AND r.status = 'ACTIVE' 
    ORDER BY lu.timestamp DESC
    """, nativeQuery = true)
    List<LocationUpdate> findNearbyLocationUpdates(@Param("latitude") Double latitude, 
                                               @Param("longitude") Double longitude, 
                                               @Param("radius") Double radius);
}
