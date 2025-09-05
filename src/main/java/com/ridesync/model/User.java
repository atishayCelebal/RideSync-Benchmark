package com.ridesync.model;

import jakarta.persistence.*;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.EqualsAndHashCode;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import java.time.LocalDateTime;
import java.util.HashSet;
import java.util.Set;
import java.util.UUID;

@Entity
@Table(name = "users", uniqueConstraints = @UniqueConstraint(columnNames = "email"))
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@EqualsAndHashCode(callSuper = false)
public class User extends BaseEntity {
    
    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    @Column(name = "id", nullable = false, updatable = false)
    private UUID id;
    
    @NotBlank
    @Size(min = 2, max = 50)
    @Column(name = "username", nullable = false, unique = true)
    private String username;
    
    @NotBlank
    @Email
    @Column(name = "email", unique = true, nullable = false)
    // BUG T01: Missing @Column(unique=true) - allows duplicate emails
    private String email;
    
    @NotBlank
    @Size(min = 6, max = 100)
    @Column(name = "password")
    // BUG T01: Will store raw password instead of hashed
    private String password;
    
    @Column(name = "first_name")
    private String firstName;
    
    @Column(name = "last_name")
    private String lastName;
    
    @Enumerated(EnumType.STRING)
    @Column(name = "role")
    @Builder.Default
    private UserRole role = UserRole.USER;
    
    @Column(name = "last_login")
    private LocalDateTime lastLogin;
    
    @Column(name = "is_active")
    @Builder.Default
    private Boolean isActive = true;
    
    @OneToMany(mappedBy = "user", cascade = CascadeType.ALL, fetch = FetchType.LAZY)
    @Builder.Default
    private Set<GroupMember> groupMemberships = new HashSet<>();
    
    @OneToMany(mappedBy = "user", cascade = CascadeType.ALL, fetch = FetchType.LAZY)
    @Builder.Default
    private Set<Ride> rides = new HashSet<>();
    
    @OneToMany(mappedBy = "user", cascade = CascadeType.ALL, fetch = FetchType.LAZY)
    @Builder.Default
    private Set<Device> devices = new HashSet<>();
    
    // Custom constructor for registration
    public User(String username, String email, String password) {
        this.username = username;
        this.email = email;
        this.password = password; // BUG T01: Raw password storage
    }
    
    @PrePersist
    @PreUpdate
    private void hashPassword() {
        if (this.password != null && !this.password.startsWith("$2a$")) {
            // Only hash if password is not already hashed (BCrypt hashes start with $2a$)
            BCryptPasswordEncoder encoder = new BCryptPasswordEncoder();
            this.password = encoder.encode(this.password);
        }
    }
}
