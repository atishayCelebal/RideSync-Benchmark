package com.ridesync.repository;

import com.ridesync.model.GroupMember;
import com.ridesync.model.GroupRole;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface GroupMemberRepository extends JpaRepository<GroupMember, UUID> {
    
    Optional<GroupMember> findByGroupIdAndUserIdAndIsActiveTrue(UUID groupId, UUID userId);
    
    List<GroupMember> findByGroupIdAndIsActiveTrue(UUID groupId);
    
    List<GroupMember> findByUserIdAndIsActiveTrue(UUID userId);
    
    @Query("SELECT gm FROM GroupMember gm WHERE gm.group.id = :groupId AND gm.role = :role AND gm.isActive = true")
    List<GroupMember> findByGroupIdAndRole(@Param("groupId") UUID groupId, @Param("role") GroupRole role);
    
    @Query("SELECT gm FROM GroupMember gm WHERE gm.group.id = :groupId AND gm.user.id = :userId AND gm.isActive = true")
    Optional<GroupMember> findActiveMembership(@Param("groupId") UUID groupId, @Param("userId") UUID userId);
}
