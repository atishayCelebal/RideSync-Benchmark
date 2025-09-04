package com.ridesync.repository;

import com.ridesync.model.Alert;
import com.ridesync.model.AlertType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

@Repository
public interface AlertRepository extends JpaRepository<Alert, UUID> {
    
    List<Alert> findByRideIdOrderByCreatedAtDesc(UUID rideId);
    
    List<Alert> findByUserIdOrderByCreatedAtDesc(UUID userId);
    
    @Query("SELECT a FROM Alert a WHERE a.ride.id = :rideId AND a.type = :type ORDER BY a.createdAt DESC")
    List<Alert> findByRideIdAndType(@Param("rideId") UUID rideId, @Param("type") AlertType type);
    
    @Query("SELECT a FROM Alert a WHERE a.ride.id = :rideId AND a.isRead = false ORDER BY a.createdAt DESC")
    List<Alert> findUnreadByRideId(@Param("rideId") UUID rideId);
    
    @Query("SELECT a FROM Alert a WHERE a.createdAt >= :since ORDER BY a.createdAt DESC")
    List<Alert> findRecentAlerts(@Param("since") LocalDateTime since);
    
    // Device-specific queries for enhanced debugging and analytics
    List<Alert> findByDeviceIdOrderByCreatedAtDesc(UUID deviceId);
    
    @Query("SELECT a FROM Alert a WHERE a.device.id = :deviceId AND a.type = :type ORDER BY a.createdAt DESC")
    List<Alert> findByDeviceIdAndType(@Param("deviceId") UUID deviceId, @Param("type") AlertType type);
    
    @Query("SELECT a FROM Alert a WHERE a.device.deviceType = :deviceType ORDER BY a.createdAt DESC")
    List<Alert> findByDeviceType(@Param("deviceType") String deviceType);
    
    @Query("SELECT a FROM Alert a WHERE a.device.osVersion = :osVersion ORDER BY a.createdAt DESC")
    List<Alert> findByDeviceOsVersion(@Param("osVersion") String osVersion);
    
    @Query("SELECT a FROM Alert a WHERE a.device.appVersion = :appVersion ORDER BY a.createdAt DESC")
    List<Alert> findByDeviceAppVersion(@Param("appVersion") String appVersion);
    
    @Query("SELECT a FROM Alert a WHERE a.device.id = :deviceId AND a.createdAt >= :since ORDER BY a.createdAt DESC")
    List<Alert> findByDeviceIdAndCreatedAtAfter(@Param("deviceId") UUID deviceId, @Param("since") LocalDateTime since);
    
    // Analytics queries for device performance
    @Query("SELECT a.device.deviceType, COUNT(a) FROM Alert a WHERE a.createdAt >= :since GROUP BY a.device.deviceType")
    List<Object[]> getAlertCountByDeviceType(@Param("since") LocalDateTime since);
    
    @Query("SELECT a.device.osVersion, COUNT(a) FROM Alert a WHERE a.createdAt >= :since GROUP BY a.device.osVersion")
    List<Object[]> getAlertCountByOsVersion(@Param("since") LocalDateTime since);
    
    @Query("SELECT a.device, COUNT(a) FROM Alert a WHERE a.createdAt >= :since GROUP BY a.device ORDER BY COUNT(a) DESC")
    List<Object[]> getMostProblematicDevices(@Param("since") LocalDateTime since);
}
