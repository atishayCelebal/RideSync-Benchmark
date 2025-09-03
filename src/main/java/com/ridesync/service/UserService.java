package com.ridesync.service;

import com.ridesync.dto.UserRegistrationDto;
import com.ridesync.model.User;
import com.ridesync.model.UserRole;
import com.ridesync.repository.UserRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

@Service
@Transactional
public class UserService {
    
    @Autowired
    private UserRepository userRepository;
    
    // BUG T01: No password hashing, duplicate emails allowed
    public User registerUser(UserRegistrationDto registrationDto) {
        // BUG T01: No duplicate email check
        User user = new User();
        user.setUsername(registrationDto.getUsername());
        user.setEmail(registrationDto.getEmail());
        user.setPassword(registrationDto.getPassword()); // BUG T01: Raw password storage
        user.setFirstName(registrationDto.getFirstName());
        user.setLastName(registrationDto.getLastName());
        user.setPhone(registrationDto.getPhone());
        user.setRole(UserRole.USER);
        user.setIsActive(true);
        user.setCreatedAt(LocalDateTime.now());
        
        return userRepository.save(user);
    }
    
    public Optional<User> findByUsername(String username) {
        return userRepository.findByUsername(username);
    }
    
    public Optional<User> findByEmail(String email) {
        return userRepository.findByEmail(email);
    }
    
    public Optional<User> findById(Long id) {
        return userRepository.findById(id);
    }
    
    public List<User> findAllActiveUsers() {
        return userRepository.findAll().stream()
                .filter(User::getIsActive)
                .toList();
    }
    
    public User updateUser(User user) {
        return userRepository.save(user);
    }
    
    public void deactivateUser(Long userId) {
        userRepository.findById(userId).ifPresent(user -> {
            user.setIsActive(false);
            userRepository.save(user);
        });
    }
    
    public boolean existsByUsername(String username) {
        return userRepository.existsByUsername(username);
    }
    
    public boolean existsByEmail(String email) {
        return userRepository.existsByEmail(email);
    }
    
    // BUG T01: No password validation method
    public boolean validatePassword(String rawPassword, String hashedPassword) {
        // BUG T01: Direct string comparison instead of BCrypt
        return rawPassword.equals(hashedPassword);
    }
}
