package com.ridesync.repository;

import com.ridesync.model.Alert;
import com.ridesync.model.AlertType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;

@Repository
public interface AlertRepository extends JpaRepository<Alert, Long> {
    
    List<Alert> findByRideIdOrderByCreatedAtDesc(Long rideId);
    
    List<Alert> findByUserIdOrderByCreatedAtDesc(Long userId);
    
    @Query("SELECT a FROM Alert a WHERE a.ride.id = :rideId AND a.type = :type ORDER BY a.createdAt DESC")
    List<Alert> findByRideIdAndType(@Param("rideId") Long rideId, @Param("type") AlertType type);
    
    @Query("SELECT a FROM Alert a WHERE a.ride.id = :rideId AND a.isRead = false ORDER BY a.createdAt DESC")
    List<Alert> findUnreadByRideId(@Param("rideId") Long rideId);
    
    @Query("SELECT a FROM Alert a WHERE a.createdAt >= :since ORDER BY a.createdAt DESC")
    List<Alert> findRecentAlerts(@Param("since") LocalDateTime since);
}
