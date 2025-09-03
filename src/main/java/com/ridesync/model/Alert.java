package com.ridesync.model;

import jakarta.persistence.*;
import jakarta.validation.constraints.NotBlank;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;
import java.time.LocalDateTime;

@Entity
@Table(name = "alerts")
@Data
@NoArgsConstructor
@AllArgsConstructor
public class Alert {
    
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "ride_id", nullable = false)
    private Ride ride;
    
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;
    
    @Enumerated(EnumType.STRING)
    @Column(name = "type")
    private AlertType type;
    
    @NotBlank
    @Column(name = "message")
    // BUG T12: Message template with broken placeholders
    private String message;
    
    @Column(name = "severity")
    private String severity = "INFO";
    
    @Column(name = "latitude")
    private Double latitude;
    
    @Column(name = "longitude")
    private Double longitude;
    
    @Column(name = "created_at")
    private LocalDateTime createdAt;
    
    @Column(name = "is_read")
    private Boolean isRead = false;
    
    // Custom constructor for creating alerts
    public Alert(Ride ride, User user, AlertType type, String message) {
        this.createdAt = LocalDateTime.now();
        this.ride = ride;
        this.user = user;
        this.type = type;
        this.message = message;
    }
    
    @Override
    public String toString() {
        return "Alert{" +
                "id=" + id +
                ", ride=" + (ride != null ? ride.getId() : null) +
                ", user=" + (user != null ? user.getId() : null) +
                ", type=" + type +
                ", message='" + message + '\'' +
                ", severity='" + severity + '\'' +
                ", isRead=" + isRead +
                '}';
    }
}
