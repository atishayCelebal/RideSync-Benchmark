package com.ridesync.model;

import jakarta.persistence.*;
import jakarta.validation.constraints.NotBlank;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.EqualsAndHashCode;
import java.util.UUID;

@Entity
@Table(name = "alerts")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@EqualsAndHashCode(callSuper = false)
public class Alert extends BaseEntity {
    
    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    @Column(name = "id", nullable = false, updatable = false)
    private UUID id;
    
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "ride_id", nullable = false)
    private Ride ride;
    
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;
    
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "device_id")
    private Device device;
    
    @Enumerated(EnumType.STRING)
    @Column(name = "type")
    private AlertType type;
    
    @NotBlank
    @Column(name = "message")
    // BUG T12: Message template with broken placeholders
    private String message;
    
    @Column(name = "severity")
    @Builder.Default
    private String severity = "INFO";
    
    @Column(name = "latitude")
    private Double latitude;
    
    @Column(name = "longitude")
    private Double longitude;
    
    @Column(name = "is_read")
    @Builder.Default
    private Boolean isRead = false;
    
    // Custom constructor for creating alerts
    public Alert(Ride ride, User user, Device device, AlertType type, String message) {
        this.ride = ride;
        this.user = user;
        this.device = device;
        this.type = type;
        this.message = message;
    }
}
