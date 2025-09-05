package com.ridesync.service;

import com.ridesync.model.Alert;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface AlertService {
    
    List<Alert> getUserAlerts(UUID userId);
    
    List<Alert> getRideAlerts(UUID rideId);
    
    List<Alert> getGroupAlerts(UUID groupId);
    
    List<Alert> getUnreadAlerts(UUID userId);
    
    Optional<Alert> findById(UUID alertId);
    
    Alert markAsRead(UUID alertId);
    
    void deleteAlert(UUID alertId);
    
    Alert createAlert(Alert alert);
}