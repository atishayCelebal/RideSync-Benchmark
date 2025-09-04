package com.ridesync.repository;

import com.ridesync.model.Device;
import com.ridesync.model.DeviceType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface DeviceRepository extends JpaRepository<Device, UUID> {
    
    Optional<Device> findByDeviceIdAndIsActiveTrue(String deviceId);
    
    List<Device> findByUserIdAndIsActiveTrue(UUID userId);
    
    List<Device> findByDeviceTypeAndIsActiveTrue(DeviceType deviceType);
    
    @Query("SELECT d FROM Device d WHERE d.user.id = :userId AND d.isActive = true ORDER BY d.lastSeen DESC")
    List<Device> findActiveByUserIdOrderByLastSeenDesc(@Param("userId") UUID userId);
    
    @Query("SELECT d FROM Device d WHERE d.lastSeen < :cutoffTime AND d.isActive = true")
    List<Device> findInactiveDevices(@Param("cutoffTime") LocalDateTime cutoffTime);
    
    @Query("SELECT d FROM Device d WHERE d.user.id = :userId AND d.deviceId = :deviceId AND d.isActive = true")
    Optional<Device> findByUserAndDeviceId(@Param("userId") UUID userId, @Param("deviceId") String deviceId);
}
