package com.ridesync.repository;

import com.ridesync.model.Group;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.UUID;

@Repository
public interface GroupRepository extends JpaRepository<Group, UUID> {
    
    @Query("SELECT g FROM Group g WHERE g.isActive = true")
    List<Group> findAllActive();
    
    @Query("SELECT g FROM Group g JOIN g.members gm WHERE gm.user.id = :userId AND gm.isActive = true AND g.isActive = true")
    List<Group> findByUserId(@Param("userId") UUID userId);
    
    @Query("SELECT g FROM Group g WHERE g.admin.id = :adminId AND g.isActive = true")
    List<Group> findByAdminId(@Param("adminId") UUID adminId);
}
