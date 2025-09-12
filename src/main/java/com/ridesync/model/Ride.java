package com.ridesync.model;

import jakarta.persistence.*;
import jakarta.validation.constraints.NotBlank;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.EqualsAndHashCode;
import java.time.LocalDateTime;
import java.util.HashSet;
import java.util.Set;
import java.util.UUID;

@Entity
@Table(name = "rides")
@Data
@NoArgsConstructor
@AllArgsConstructor
@EqualsAndHashCode(callSuper = false)
public class Ride extends BaseEntity {
    
    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    @Column(name = "id", nullable = false, updatable = false)
    private UUID id;
    
    @NotBlank
    @Column(name = "name")
    private String name;    
    
    @Column(name = "description")
    private String description;
    
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "group_id", nullable = false)
    private Group group;
    
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;
    
    @Enumerated(EnumType.STRING)
    @Column(name = "status")
    private RideStatus status = RideStatus.PLANNED;
    
    @Column(name = "start_time")
    private LocalDateTime startTime;
    
    @Column(name = "end_time")
    private LocalDateTime endTime;
    
    @Column(name = "is_active")
    private Boolean isActive = true;
    
    @OneToMany(mappedBy = "ride", cascade = CascadeType.ALL, fetch = FetchType.LAZY)
    private Set<LocationUpdate> locationUpdates = new HashSet<>();
    
    @OneToMany(mappedBy = "ride", cascade = CascadeType.ALL, fetch = FetchType.LAZY)
    private Set<Alert> alerts = new HashSet<>();
    
    // Custom constructor for creating rides
    public Ride(String name, String description, Group group, User createdBy) {
        this.name = name;
        this.description = description;
        this.group = group;
        this.user = createdBy;
    }
    
    public static RideBuilder builder() {
        return new RideBuilder();
    }
    public static class RideBuilder {
        private UUID id;
        private String name;
        private String description;
        private Group group;
        private User user;
        private RideStatus status = RideStatus.PLANNED;
        private LocalDateTime startTime;
        private LocalDateTime endTime;
        private Boolean isActive = true;
        private Set<LocationUpdate> locationUpdates = new HashSet<>();
        private Set<Alert> alerts = new HashSet<>();
        
        public RideBuilder id(UUID id) { this.id = id; return this; }
        public RideBuilder name(String name) { this.name = name; return this; }
        public RideBuilder description(String description) { this.description = description; return this; }
        public RideBuilder group(Group group) { this.group = group; return this; }
        public RideBuilder user(User user) { this.user = user; return this; }
        public RideBuilder createdBy(User user) { this.user = user; return this; }
        public RideBuilder status(RideStatus status) { this.status = status; return this; }
        public RideBuilder startTime(LocalDateTime startTime) { this.startTime = startTime; return this; }
        public RideBuilder endTime(LocalDateTime endTime) { this.endTime = endTime; return this; }
        public RideBuilder isActive(Boolean isActive) { this.isActive = isActive; return this; }
        public RideBuilder locationUpdates(Set<LocationUpdate> locationUpdates) { this.locationUpdates = locationUpdates; return this; }
        public RideBuilder alerts(Set<Alert> alerts) { this.alerts = alerts; return this; }
        
        public Ride build() {
            return new Ride(id, name, description, group, user, status, startTime, endTime, isActive, locationUpdates, alerts);
        }
    }
}
