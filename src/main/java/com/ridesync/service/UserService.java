package com.ridesync.service;

import com.ridesync.dto.UserRegistrationDto;
import com.ridesync.dto.UserUpdateDto;
import com.ridesync.model.User;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface UserService {
    
    User registerUser(UserRegistrationDto registrationDto);
    
    Optional<User> findByUsername(String username);
    
    Optional<User> findByEmail(String email);
    
    Optional<User> findById(UUID id);
    
    List<User> findAllActiveUsers();
    
    User updateUser(User user);
    
    void deactivateUser(UUID userId);
    
    User activateUser(UUID userId);
    
    User updateUser(UUID userId, UserUpdateDto userUpdateDto);
    
    boolean existsByUsername(String username);
    
    boolean existsByEmail(String email);
    
    boolean validatePassword(String rawPassword, String hashedPassword);
}