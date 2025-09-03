package com.ridesync.model;

import jakarta.persistence.*;
import jakarta.validation.constraints.NotNull;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;
import java.time.LocalDateTime;

@Entity
@Table(name = "location_updates")
@Data
@NoArgsConstructor
@AllArgsConstructor
public class LocationUpdate {
    
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "ride_id", nullable = false)
    private Ride ride;
    
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;
    
    @NotNull
    @Column(name = "latitude")
    private Double latitude;
    
    @NotNull
    @Column(name = "longitude")
    private Double longitude;
    
    @Column(name = "altitude")
    private Double altitude;
    
    @Column(name = "speed")
    private Double speed;
    
    @Column(name = "heading")
    private Double heading;
    
    @Column(name = "accuracy")
    private Double accuracy;
    
    @Column(name = "device_id")
    // BUG T08: Using deviceId instead of userId for tracking
    private String deviceId;
    
    @Column(name = "timestamp")
    private LocalDateTime timestamp;
    
    @Column(name = "created_at")
    private LocalDateTime createdAt;
    
    // Custom constructor for location updates
    public LocationUpdate(Ride ride, User user, Double latitude, Double longitude, String deviceId) {
        this.createdAt = LocalDateTime.now();
        this.timestamp = LocalDateTime.now();
        this.ride = ride;
        this.user = user;
        this.latitude = latitude;
        this.longitude = longitude;
        this.deviceId = deviceId; // BUG T08: Using deviceId
    }
    
    @Override
    public String toString() {
        return "LocationUpdate{" +
                "id=" + id +
                ", ride=" + (ride != null ? ride.getId() : null) +
                ", user=" + (user != null ? user.getId() : null) +
                ", latitude=" + latitude +
                ", longitude=" + longitude +
                ", deviceId='" + deviceId + '\'' +
                ", timestamp=" + timestamp +
                '}';
    }
}
