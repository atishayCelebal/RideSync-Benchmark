package com.ridesync.model;

import jakarta.persistence.*;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.EqualsAndHashCode;

import java.util.UUID;

@Entity
@Table(name = "devices")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@EqualsAndHashCode(callSuper = false)
public class Device extends BaseEntity {
    
    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;
    
    @NotBlank
    @Size(min = 1, max = 100)
    @Column(name = "device_name", nullable = false)
    private String deviceName;
    
    @NotBlank
    @Size(min = 1, max = 100)
    @Column(name = "device_id", nullable = false, unique = true)
    private String deviceId; // Unique device identifier (e.g., IMEI, MAC address)
    
    @Column(name = "device_type")
    @Enumerated(EnumType.STRING)
    @Builder.Default
    private DeviceType deviceType = DeviceType.MOBILE;
    
    @Column(name = "os_version")
    private String osVersion;
    
    @Column(name = "app_version")
    private String appVersion;
    
    @Column(name = "gps_accuracy")
    private Double gpsAccuracy; // GPS accuracy in meters
    
    @Column(name = "last_seen")
    private java.time.LocalDateTime lastSeen;
    
    @Column(name = "is_active")
    @Builder.Default
    private Boolean isActive = true;
    
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;
    
    // Custom constructor
    public Device(String deviceName, String deviceId, DeviceType deviceType, User user) {
        this.deviceName = deviceName;
        this.deviceId = deviceId;
        this.deviceType = deviceType;
        this.user = user;
    }
}
