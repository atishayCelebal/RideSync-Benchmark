package com.ridesync.controller;

import com.ridesync.dto.ApiResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.time.LocalDateTime;
import java.util.Map;

@RestController
@RequestMapping("/api/v1")
@RequiredArgsConstructor
public class VersionController {
    
    @Value("${app.version:1.0.0}")
    private String appVersion;
    
    @Value("${spring.application.name:RideSync}")
    private String appName;
    
    @GetMapping("/version")
    public ResponseEntity<ApiResponse<Map<String, Object>>> getVersion() {
        Map<String, Object> versionInfo = Map.of(
            "application", appName,
            "version", appVersion,
            "apiVersion", "v1",
            "timestamp", LocalDateTime.now(),
            "status", "active"
        );
        
        return ResponseEntity.ok(ApiResponse.success("Version information retrieved successfully", versionInfo));
    }
    
    @GetMapping("/health")
    public ResponseEntity<ApiResponse<Map<String, Object>>> getHealth() {
        Map<String, Object> healthInfo = Map.of(
            "status", "UP",
            "timestamp", LocalDateTime.now(),
            "version", appVersion,
            "apiVersion", "v1"
        );
        
        return ResponseEntity.ok(ApiResponse.success("Health check successful", healthInfo));
    }
}
