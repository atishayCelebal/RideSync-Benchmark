package com.ridesync.repository;

import com.ridesync.model.LocationUpdate;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;

@Repository
public interface LocationUpdateRepository extends JpaRepository<LocationUpdate, Long> {
    
    List<LocationUpdate> findByRideIdOrderByTimestampDesc(Long rideId);
    
    List<LocationUpdate> findByUserIdOrderByTimestampDesc(Long userId);
    
    @Query("SELECT lu FROM LocationUpdate lu WHERE lu.ride.id = :rideId AND lu.timestamp >= :since ORDER BY lu.timestamp DESC")
    List<LocationUpdate> findByRideIdAndTimestampAfter(@Param("rideId") Long rideId, @Param("since") LocalDateTime since);
    
    @Query("SELECT lu FROM LocationUpdate lu WHERE lu.ride.id = :rideId AND lu.user.id = :userId ORDER BY lu.timestamp DESC")
    List<LocationUpdate> findByRideIdAndUserId(@Param("rideId") Long rideId, @Param("userId") Long userId);
    
    // BUG T13: Unrestricted query - no group filtering
    @Query("SELECT lu FROM LocationUpdate lu WHERE lu.ride.status = 'ACTIVE' ORDER BY lu.timestamp DESC")
    List<LocationUpdate> findAllActiveLocationUpdates();
    
    @Query("SELECT lu FROM LocationUpdate lu WHERE lu.ride.id = :rideId AND lu.deviceId = :deviceId ORDER BY lu.timestamp DESC")
    List<LocationUpdate> findByRideIdAndDeviceId(@Param("rideId") Long rideId, @Param("deviceId") String deviceId);
    
    // BUG T16: Inefficient nearby query - no bounding box filter
    @Query("SELECT lu FROM LocationUpdate lu WHERE " +
           "ST_DWithin(ST_GeogFromText(CONCAT('POINT(', lu.longitude, ' ', lu.latitude, ')')), " +
           "ST_GeogFromText(CONCAT('POINT(', :longitude, ' ', :latitude, ')')), :radius) " +
           "AND lu.ride.status = 'ACTIVE' " +
           "ORDER BY lu.timestamp DESC")
    List<LocationUpdate> findNearbyLocationUpdates(@Param("latitude") Double latitude, 
                                                   @Param("longitude") Double longitude, 
                                                   @Param("radius") Double radius);
}
