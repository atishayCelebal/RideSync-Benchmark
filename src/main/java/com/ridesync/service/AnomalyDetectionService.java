package com.ridesync.service;

import com.ridesync.model.Alert;
import com.ridesync.model.LocationUpdate;

import java.util.List;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;

public interface AnomalyDetectionService {
    
    void detectAnomalies(UUID rideId);
    
    CompletableFuture<Void> detectAnomaliesAsync(UUID rideId);
    
    void detectStationaryAnomaly(UUID userId, List<LocationUpdate> updates);
    
    void detectDirectionDrift(UUID userId, List<LocationUpdate> updates);
    
    void createAlert(Alert alert);
}