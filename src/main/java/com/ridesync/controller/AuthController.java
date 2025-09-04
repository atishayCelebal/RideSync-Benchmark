package com.ridesync.controller;

import com.ridesync.dto.ApiResponse;
import com.ridesync.dto.AuthResponseDto;
import com.ridesync.dto.UserDto;
import com.ridesync.dto.UserRegistrationDto;
import com.ridesync.mapper.UserMapper;
import com.ridesync.model.User;
import com.ridesync.security.JwtUtil;
import com.ridesync.service.UserService;
import com.ridesync.exception.ResourceNotFoundException;
import org.springframework.security.authentication.BadCredentialsException;
import jakarta.validation.Valid;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequestMapping("/api/auth")
@CrossOrigin(origins = "*")
public class AuthController {
    
    @Autowired
    private UserService userService;
    
    @Autowired
    private JwtUtil jwtUtil;
    
    @Autowired
    private UserMapper userMapper;
    
    // BUG T01: Broken User Registration – No password hashing, duplicate emails allowed
    @PostMapping("/register")
    public ResponseEntity<ApiResponse<UserDto>> registerUser(@Valid @RequestBody UserRegistrationDto registrationDto) {
        // BUG T01: No duplicate email check before registration
        User user = userService.registerUser(registrationDto);
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.success("User registered successfully", userMapper.toUserDto(user)));
    }
    
    @PostMapping("/login")
    public ResponseEntity<ApiResponse<AuthResponseDto>> loginUser(@RequestBody Map<String, String> loginRequest) {
        String username = loginRequest.get("username");
        String password = loginRequest.get("password");
        
        User user = userService.findByUsername(username)
                .orElseThrow(() -> new ResourceNotFoundException("User not found with username: " + username));
        
        // BUG T01: Direct password comparison instead of BCrypt
        if (!userService.validatePassword(password, user.getPassword())) {
            throw new BadCredentialsException("Invalid credentials");
        }
        
        // Generate JWT token with username as subject
        String token = jwtUtil.generateToken(user.getUsername());
        return ResponseEntity.status(HttpStatus.ACCEPTED).body(ApiResponse.success("Login successful", userMapper.toAuthResponseDto(user, token)));
    }
}
