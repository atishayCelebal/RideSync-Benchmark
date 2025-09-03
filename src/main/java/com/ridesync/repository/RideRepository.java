package com.ridesync.repository;

import com.ridesync.model.Ride;
import com.ridesync.model.RideStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface RideRepository extends JpaRepository<Ride, Long> {
    
    List<Ride> findByGroupIdAndIsActiveTrue(Long groupId);
    
    List<Ride> findByUserIdAndIsActiveTrue(Long userId);
    
    @Query("SELECT r FROM Ride r WHERE r.group.id = :groupId AND r.status = :status AND r.isActive = true")
    List<Ride> findByGroupIdAndStatus(@Param("groupId") Long groupId, @Param("status") RideStatus status);
    
    @Query("SELECT r FROM Ride r WHERE r.status = :status AND r.isActive = true")
    List<Ride> findByStatus(@Param("status") RideStatus status);
    
    @Query("SELECT r FROM Ride r WHERE r.group.id = :groupId AND r.isActive = true ORDER BY r.createdAt DESC")
    List<Ride> findActiveByGroupIdOrderByCreatedAtDesc(@Param("groupId") Long groupId);
}
