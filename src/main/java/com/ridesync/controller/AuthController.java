package com.ridesync.controller;

import com.ridesync.dto.ApiResponse;
import com.ridesync.dto.LoginRequestDto;
import com.ridesync.dto.RefreshTokenRequestDto;
import com.ridesync.dto.TokenResponseDto;
import com.ridesync.dto.UserDto;
import com.ridesync.dto.UserRegistrationDto;
import com.ridesync.mapper.UserMapper;
import com.ridesync.model.User;
import com.ridesync.security.JwtUtil;
import com.ridesync.service.RefreshTokenService;
import com.ridesync.service.UserService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/auth")
@RequiredArgsConstructor
public class AuthController {
    
    private final UserService userService;
    private final JwtUtil jwtUtil;
    private final UserMapper userMapper;
    private final AuthenticationManager authenticationManager;
    private final RefreshTokenService refreshTokenService;
    
    // BUG T01: Broken User Registration – No password hashing, duplicate emails allowed
    @PostMapping("/register")
    public ResponseEntity<ApiResponse<UserDto>> registerUser(@Valid @RequestBody UserRegistrationDto registrationDto) {
        // BUG T01: No duplicate email check before registration
        User user = userService.registerUser(registrationDto);
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.success("User registered successfully", userMapper.toUserDto(user)));
    }
    
    @PostMapping("/login")
    public ResponseEntity<ApiResponse<TokenResponseDto>> loginUser(@Valid @RequestBody LoginRequestDto loginRequest) {
        // Use Spring Security's AuthenticationManager for proper authentication
        Authentication authentication = authenticationManager.authenticate(
                new UsernamePasswordAuthenticationToken(loginRequest.getUsername(), loginRequest.getPassword())
        );
        
        // Get user details from the authenticated principal (more efficient)
        User user = (User) authentication.getPrincipal();
        
        // Generate access token (short-lived)
        String accessToken = jwtUtil.generateAccessToken(authentication.getName());
        
        // Generate refresh token (long-lived) and store in database
        String refreshToken = refreshTokenService.createRefreshToken(user).getToken();
        
        // Create token response
        TokenResponseDto tokenResponse = TokenResponseDto.builder()
                .accessToken(accessToken)
                .refreshToken(refreshToken)
                .tokenType("Bearer")
                .expiresIn(86400L) // 24 hours
                .build();
        
        return ResponseEntity.ok(ApiResponse.success("Login successful", tokenResponse));
    }
    
    @PostMapping("/refresh")
    public ResponseEntity<ApiResponse<TokenResponseDto>> refreshToken(@Valid @RequestBody RefreshTokenRequestDto request) {
        // Verify refresh token
        var refreshToken = refreshTokenService.refreshToken(request.getRefreshToken());
        
        // Generate new access token
        String newAccessToken = jwtUtil.generateAccessToken(refreshToken.getUser().getUsername());
        
        // Create token response
        TokenResponseDto tokenResponse = TokenResponseDto.builder()
                .accessToken(newAccessToken)
                .refreshToken(request.getRefreshToken()) // Keep the same refresh token
                .tokenType("Bearer")
                .expiresIn(86400L) // 24 hours
                .build();
        
        return ResponseEntity.ok(ApiResponse.success("Token refreshed successfully", tokenResponse));
    }
    
    @PostMapping("/logout")
    public ResponseEntity<ApiResponse<String>> logout(@Valid @RequestBody RefreshTokenRequestDto request) {
        // Revoke the refresh token
        refreshTokenService.revokeToken(request.getRefreshToken());
        
        return ResponseEntity.ok(ApiResponse.success("Logout successful"));
    }
}
