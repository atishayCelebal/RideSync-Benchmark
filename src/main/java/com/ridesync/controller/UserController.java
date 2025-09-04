package com.ridesync.controller;

import com.ridesync.dto.ApiResponse;
import com.ridesync.dto.SimpleResponseDto;
import com.ridesync.dto.UserDto;
import com.ridesync.dto.UserUpdateDto;
import com.ridesync.mapper.UserMapper;
import com.ridesync.model.User;
import com.ridesync.service.UserService;
import com.ridesync.util.SecurityUtil;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.UUID;

@RestController
@RequestMapping("/api/v1/users")
@RequiredArgsConstructor
public class UserController {
    
    private final UserService userService;
    private final UserMapper userMapper;
    
    @GetMapping("/profile")
    public ResponseEntity<ApiResponse<UserDto>> getUserProfile() {
        User user = SecurityUtil.getCurrentUser();
        return ResponseEntity.ok(ApiResponse.success("User profile retrieved successfully", 
                userMapper.toUserDto(user)));
    }
    
    @PutMapping("/profile")
    public ResponseEntity<ApiResponse<UserDto>> updateUserProfile(@Valid @RequestBody UserUpdateDto userUpdateDto) {
        UUID userId = SecurityUtil.getCurrentUserId();
        User user = userService.updateUser(userId, userUpdateDto);
        return ResponseEntity.ok(ApiResponse.success("User profile updated successfully", 
                userMapper.toUserDto(user)));
    }
    
    @GetMapping("/{userId}")
    public ResponseEntity<ApiResponse<UserDto>> getUser(@PathVariable UUID userId) {
        User user = userService.findById(userId)
                .orElseThrow(() -> new com.ridesync.exception.ResourceNotFoundException("User", "id", userId));
        return ResponseEntity.ok(ApiResponse.success("User retrieved successfully", 
                userMapper.toUserDto(user)));
    }
    
    @PutMapping("/{userId}")
    public ResponseEntity<ApiResponse<UserDto>> updateUser(@PathVariable UUID userId, 
                                                          @Valid @RequestBody UserUpdateDto userUpdateDto) {
        User user = userService.updateUser(userId, userUpdateDto);
        return ResponseEntity.ok(ApiResponse.success("User updated successfully", 
                userMapper.toUserDto(user)));
    }
    
    @DeleteMapping("/{userId}")
    public ResponseEntity<ApiResponse<SimpleResponseDto>> deactivateUser(@PathVariable UUID userId) {
        userService.deactivateUser(userId);
        SimpleResponseDto response = SimpleResponseDto.builder()
                .message("User deactivated successfully")
                .build();
        return ResponseEntity.ok(ApiResponse.success("User deactivated successfully", response));
    }
    
    @PutMapping("/{userId}/activate")
    public ResponseEntity<ApiResponse<UserDto>> activateUser(@PathVariable UUID userId) {
        User user = userService.activateUser(userId);
        return ResponseEntity.ok(ApiResponse.success("User activated successfully", 
                userMapper.toUserDto(user)));
    }
}
