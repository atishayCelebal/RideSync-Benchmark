package com.ridesync.model;

import jakarta.persistence.*;
import jakarta.validation.constraints.NotBlank;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;
import java.time.LocalDateTime;
import java.util.HashSet;
import java.util.Set;

@Entity
@Table(name = "rides")
@Data
@NoArgsConstructor
@AllArgsConstructor
public class Ride {
    
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    
    @NotBlank
    @Column(name = "name")
    private String name;
    
    @Column(name = "description")
    private String description;
    
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "group_id", nullable = false)
    private Group group;
    
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "created_by", nullable = false)
    private User user;
    
    @Enumerated(EnumType.STRING)
    @Column(name = "status")
    private RideStatus status = RideStatus.PLANNED;
    
    @Column(name = "start_time")
    private LocalDateTime startTime;
    
    @Column(name = "end_time")
    private LocalDateTime endTime;
    
    @Column(name = "created_at")
    private LocalDateTime createdAt;
    
    @Column(name = "is_active")
    private Boolean isActive = true;
    
    @OneToMany(mappedBy = "ride", cascade = CascadeType.ALL, fetch = FetchType.LAZY)
    private Set<LocationUpdate> locationUpdates = new HashSet<>();
    
    @OneToMany(mappedBy = "ride", cascade = CascadeType.ALL, fetch = FetchType.LAZY)
    private Set<Alert> alerts = new HashSet<>();
    
    // Custom constructor for creating rides
    public Ride(String name, String description, Group group, User user) {
        this.createdAt = LocalDateTime.now();
        this.name = name;
        this.description = description;
        this.group = group;
        this.user = user;
    }
    

    
    @Override
    public String toString() {
        return "Ride{" +
                "id=" + id +
                ", name='" + name + '\'' +
                ", group=" + (group != null ? group.getId() : null) +
                ", user=" + (user != null ? user.getId() : null) +
                ", status=" + status +
                ", isActive=" + isActive +
                '}';
    }
}
