package com.ridesync.dto;

import jakarta.validation.constraints.NotNull;
import java.time.LocalDateTime;

public class LocationUpdateDto {
    
    @NotNull
    private Long userId;
    
    @NotNull
    private Long rideId;
    
    @NotNull
    private Double latitude;
    
    @NotNull
    private Double longitude;
    
    private Double altitude;
    private Double speed;
    private Double heading;
    private Double accuracy;
    private String deviceId;
    private LocalDateTime timestamp;
    
    // Constructors
    public LocationUpdateDto() {}
    
    public LocationUpdateDto(Long userId, Long rideId, Double latitude, Double longitude) {
        this.userId = userId;
        this.rideId = rideId;
        this.latitude = latitude;
        this.longitude = longitude;
        this.timestamp = LocalDateTime.now();
    }
    
    // Getters and Setters
    public Long getUserId() {
        return userId;
    }
    
    public void setUserId(Long userId) {
        this.userId = userId;
    }
    
    public Long getRideId() {
        return rideId;
    }
    
    public void setRideId(Long rideId) {
        this.rideId = rideId;
    }
    
    public Double getLatitude() {
        return latitude;
    }
    
    public void setLatitude(Double latitude) {
        this.latitude = latitude;
    }
    
    public Double getLongitude() {
        return longitude;
    }
    
    public void setLongitude(Double longitude) {
        this.longitude = longitude;
    }
    
    public Double getAltitude() {
        return altitude;
    }
    
    public void setAltitude(Double altitude) {
        this.altitude = altitude;
    }
    
    public Double getSpeed() {
        return speed;
    }
    
    public void setSpeed(Double speed) {
        this.speed = speed;
    }
    
    public Double getHeading() {
        return heading;
    }
    
    public void setHeading(Double heading) {
        this.heading = heading;
    }
    
    public Double getAccuracy() {
        return accuracy;
    }
    
    public void setAccuracy(Double accuracy) {
        this.accuracy = accuracy;
    }
    
    public String getDeviceId() {
        return deviceId;
    }
    
    public void setDeviceId(String deviceId) {
        this.deviceId = deviceId;
    }
    
    public LocalDateTime getTimestamp() {
        return timestamp;
    }
    
    public void setTimestamp(LocalDateTime timestamp) {
        this.timestamp = timestamp;
    }
    
    @Override
    public String toString() {
        return "LocationUpdateDto{" +
                "userId=" + userId +
                ", rideId=" + rideId +
                ", latitude=" + latitude +
                ", longitude=" + longitude +
                ", deviceId='" + deviceId + '\'' +
                ", timestamp=" + timestamp +
                '}';
    }
}
