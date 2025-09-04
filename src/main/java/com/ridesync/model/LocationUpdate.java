package com.ridesync.model;

import jakarta.persistence.*;
import jakarta.validation.constraints.NotNull;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.EqualsAndHashCode;
import java.time.LocalDateTime;
import java.util.UUID;

@Entity
@Table(name = "location_updates")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@EqualsAndHashCode(callSuper = false)
public class LocationUpdate extends BaseEntity {
    
    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;
    
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
    
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "device_id")
    private Device device;
    
    @Column(name = "timestamp")
    private LocalDateTime timestamp;
    
    // Custom constructor for location updates
    public LocationUpdate(Ride ride, User user, Double latitude, Double longitude, Device device) {
        this.timestamp = LocalDateTime.now();
        this.ride = ride;
        this.user = user;
        this.latitude = latitude;
        this.longitude = longitude;
        this.device = device;
    }
}
