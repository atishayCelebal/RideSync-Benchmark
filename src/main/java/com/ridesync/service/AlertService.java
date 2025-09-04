package com.ridesync.service;

import com.ridesync.exception.ResourceNotFoundException;
import com.ridesync.model.Alert;
import com.ridesync.repository.AlertRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Service
@Transactional
@RequiredArgsConstructor
public class AlertService {
    
    private final AlertRepository alertRepository;
    
    public List<Alert> getUserAlerts(UUID userId) {
        return alertRepository.findByUserIdOrderByCreatedAtDesc(userId);
    }
    
    public List<Alert> getRideAlerts(UUID rideId) {
        return alertRepository.findByRideIdOrderByCreatedAtDesc(rideId);
    }
    
    public List<Alert> getGroupAlerts(UUID groupId) {
        return alertRepository.findByGroupIdOrderByCreatedAtDesc(groupId);
    }
    
    public List<Alert> getUnreadAlerts(UUID userId) {
        return alertRepository.findByUserIdAndIsReadFalseOrderByCreatedAtDesc(userId);
    }
    
    public Optional<Alert> findById(UUID alertId) {
        return alertRepository.findById(alertId);
    }
    
    public Alert markAsRead(UUID alertId) {
        Alert alert = alertRepository.findById(alertId)
                .orElseThrow(() -> new ResourceNotFoundException("Alert", "id", alertId));
        
        alert.setIsRead(true);
        alert.setUpdatedAt(LocalDateTime.now());
        
        return alertRepository.save(alert);
    }
    
    public void deleteAlert(UUID alertId) {
        Alert alert = alertRepository.findById(alertId)
                .orElseThrow(() -> new ResourceNotFoundException("Alert", "id", alertId));
        
        alertRepository.delete(alert);
    }
    
    public Alert createAlert(Alert alert) {
        return alertRepository.save(alert);
    }
}
