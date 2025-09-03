package com.ridesync.repository;

import com.ridesync.model.GroupMember;
import com.ridesync.model.GroupRole;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface GroupMemberRepository extends JpaRepository<GroupMember, Long> {
    
    Optional<GroupMember> findByGroupIdAndUserIdAndIsActiveTrue(Long groupId, Long userId);
    
    List<GroupMember> findByGroupIdAndIsActiveTrue(Long groupId);
    
    List<GroupMember> findByUserIdAndIsActiveTrue(Long userId);
    
    @Query("SELECT gm FROM GroupMember gm WHERE gm.group.id = :groupId AND gm.role = :role AND gm.isActive = true")
    List<GroupMember> findByGroupIdAndRole(@Param("groupId") Long groupId, @Param("role") GroupRole role);
    
    @Query("SELECT gm FROM GroupMember gm WHERE gm.group.id = :groupId AND gm.user.id = :userId AND gm.isActive = true")
    Optional<GroupMember> findActiveMembership(@Param("groupId") Long groupId, @Param("userId") Long userId);
}
