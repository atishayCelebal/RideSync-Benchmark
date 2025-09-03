package com.ridesync.model;

import jakarta.persistence.*;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;
import java.time.LocalDateTime;

@Entity
@Table(name = "group_members")
@Data
@NoArgsConstructor
@AllArgsConstructor
public class GroupMember {
    
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "group_id", nullable = false)
    private Group group;
    
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;
    
    @Enumerated(EnumType.STRING)
    @Column(name = "role")
    private GroupRole role = GroupRole.MEMBER;
    
    @Column(name = "joined_at")
    private LocalDateTime joinedAt;
    
    @Column(name = "is_active")
    private Boolean isActive = true;
    
    // Custom constructor for creating group members
    public GroupMember(Group group, User user, GroupRole role) {
        this.joinedAt = LocalDateTime.now();
        this.group = group;
        this.user = user;
        this.role = role;
    }
    
    @Override
    public String toString() {
        return "GroupMember{" +
                "id=" + id +
                ", group=" + (group != null ? group.getId() : null) +
                ", user=" + (user != null ? user.getId() : null) +
                ", role=" + role +
                ", isActive=" + isActive +
                '}';
    }
}
