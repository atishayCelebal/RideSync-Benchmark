package com.ridesync.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.ridesync.model.Alert;
import com.ridesync.model.AlertType;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.*;

/**
 * Service for processing LLM responses and creating alerts
 */
@Service
@RequiredArgsConstructor
public class LLMResponseProcessor {
    
    private static final Logger logger = LoggerFactory.getLogger(LLMResponseProcessor.class);
    
    private final ObjectMapper objectMapper;
    
    /**
     * Process LLM response and extract anomalies
     * @param llmResponse The JSON response from LLM
     * @return List of Alert objects created from LLM analysis
     */
    public List<Alert> processLLMResponse(String llmResponse) {
        logger.info("Processing LLM response");
        
        List<Alert> alerts = new ArrayList<>();
        
        try {
            // Clean the response before processing
            String cleanedResponse = cleanLLMResponse(llmResponse);
            JsonNode responseNode = objectMapper.readTree(cleanedResponse);
            
            // Extract anomalies array
            JsonNode anomaliesNode = responseNode.get("anomalies");
            if (anomaliesNode != null && anomaliesNode.isArray()) {
                for (JsonNode anomalyNode : anomaliesNode) {
                    Alert alert = createAlertFromAnomaly(anomalyNode);
                    if (alert != null) {
                        alerts.add(alert);
                    }
                }
            }
            
            // Log overall assessment
            JsonNode assessmentNode = responseNode.get("overallAssessment");
            if (assessmentNode != null) {
                logger.info("LLM Overall Assessment: {}", assessmentNode.asText());
            }
            
            JsonNode riskLevelNode = responseNode.get("riskLevel");
            if (riskLevelNode != null) {
                logger.info("LLM Risk Level: {}", riskLevelNode.asText());
            }
            
            logger.info("Successfully processed LLM response, created {} alerts", alerts.size());
            
        } catch (Exception e) {
            logger.error("Error processing LLM response: {}", e.getMessage(), e);
            // Return empty list on error - don't fail the entire process
        }
        
        return alerts;
    }
    
    /**
     * Create an Alert object from LLM anomaly data
     * @param anomalyNode The anomaly JSON node
     * @return Alert object or null if creation fails
     */
    private Alert createAlertFromAnomaly(JsonNode anomalyNode) {
        try {
            // Extract anomaly data
            String type = anomalyNode.get("type").asText();
            String severity = anomalyNode.get("severity").asText();
            String description = anomalyNode.get("description").asText();
            String userIdStr = anomalyNode.get("userId").asText();
            String timestampStr = anomalyNode.get("timestamp").asText();
            Double confidence = anomalyNode.has("confidence") ? 
                    anomalyNode.get("confidence").asDouble() : 0.0;
            
            // Map LLM anomaly type to our AlertType enum
            AlertType alertType = mapLLMAnomalyType(type);
            if (alertType == null) {
                logger.warn("Unknown LLM anomaly type: {}", type);
                return null;
            }
            
            // Create alert builder
            Alert alertBuilder = Alert.builder()
                    .type(alertType)
                    .message(description)
                    .severity(severity)
                    .isRead(false)
                    // .createdAt(LocalDateTime.now())
                    // .bui;
                    .build();
            
            // Add confidence as additional metadata in message
            if (confidence > 0) {
                String enhancedMessage = String.format("%s (Confidence: %.2f)", description, confidence);
                alertBuilder.setMessage(enhancedMessage);
            }
            
            // Add recommendations if available
            JsonNode recommendationsNode = anomalyNode.get("recommendations");
            if (recommendationsNode != null && recommendationsNode.isArray()) {
                StringBuilder recommendations = new StringBuilder();
                for (JsonNode recNode : recommendationsNode) {
                    if (recommendations.length() > 0) {
                        recommendations.append("; ");
                    }
                    recommendations.append(recNode.asText());
                }
                
                String finalMessage = String.format("%s | Recommendations: %s", 
                        alertBuilder.getMessage(), recommendations.toString());
                alertBuilder.setMessage(finalMessage);
            }
            
            // Alert alert = alertBuilder.build();
            logger.info("Created alert from LLM anomaly: {} - {}", alertType, description);
            
            return alertBuilder;
            
        } catch (Exception e) {
            logger.error("Error creating alert from LLM anomaly: {}", e.getMessage(), e);
            return null;
        }
    }
    
    /**
     * Map LLM anomaly types to our AlertType enum
     * @param llmType The LLM anomaly type
     * @return Corresponding AlertType or null if unknown
     */
    private AlertType mapLLMAnomalyType(String llmType) {
        switch (llmType.toUpperCase()) {
            case "STATIONARY_ANOMALY":
            case "STATIONARY":
                return AlertType.STATIONARY;
            case "SPEED_ANOMALY":
            case "SPEED":
                return AlertType.SPEED_ANOMALY;
            case "DIRECTION_DRIFT":
            case "ROUTE_DEVIATION":
                return AlertType.DIRECTION_DRIFT;
            case "LOCATION_ANOMALY":
            case "GPS_ANOMALY":
                return AlertType.LOCATION_ANOMALY;
            case "GROUP_COORDINATION":
            case "GROUP_ANOMALY":
                return AlertType.LOCATION_ANOMALY; // Map to closest existing type
            case "EMERGENCY":
                return AlertType.EMERGENCY;
            default:
                logger.warn("Unknown LLM anomaly type: {}", llmType);
                return AlertType.LOCATION_ANOMALY; // Default fallback
        }
    }
    
    /**
     * Validate LLM response format
     * @param llmResponse The response to validate
     * @return true if response is valid, false otherwise
     */
    public boolean validateLLMResponse(String llmResponse) {
        try {
            // Clean the response - remove any text before JSON
            String cleanedResponse = cleanLLMResponse(llmResponse);
            JsonNode responseNode = objectMapper.readTree(cleanedResponse);
            
            // Check required fields
            if (!responseNode.has("anomalies") || !responseNode.get("anomalies").isArray()) {
                logger.warn("LLM response missing or invalid 'anomalies' field");
                return false;
            }
            
            // Validate each anomaly
            JsonNode anomaliesNode = responseNode.get("anomalies");
            for (JsonNode anomalyNode : anomaliesNode) {
                if (!anomalyNode.has("type") || !anomalyNode.has("description")) {
                    logger.warn("LLM anomaly missing required fields");
                    return false;
                }
            }
            
            return true;
            
        } catch (Exception e) {
            logger.error("Error validating LLM response: {}", e.getMessage(), e);
            return false;
        }
    }
    
    /**
     * Clean LLM response by extracting JSON from text
     * @param llmResponse Raw LLM response
     * @return Cleaned JSON string
     */
    private String cleanLLMResponse(String llmResponse) {
        if (llmResponse == null || llmResponse.trim().isEmpty()) {
            return "{}";
        }
        
        // Remove common prefixes that TinyLlama adds
        String cleaned = llmResponse.trim();
        
        // Remove "Response:" prefix
        if (cleaned.startsWith("Response:")) {
            cleaned = cleaned.substring("Response:".length()).trim();
        }
        
        // Remove "Response:\n" prefix (with newline)
        if (cleaned.startsWith("Response:\n")) {
            cleaned = cleaned.substring("Response:\n".length()).trim();
        }
        
        // Remove "Here's" or similar prefixes
        if (cleaned.startsWith("Here's") || cleaned.startsWith("Here is")) {
            // Find the first '[' or '{' character
            int jsonStart = Math.max(cleaned.indexOf('['), cleaned.indexOf('{'));
            if (jsonStart > 0) {
                cleaned = cleaned.substring(jsonStart);
            }
        }
        
        // Find the first JSON object/array
        int jsonStart = Math.max(cleaned.indexOf('['), cleaned.indexOf('{'));
        if (jsonStart > 0) {
            cleaned = cleaned.substring(jsonStart);
        }
        
        // Find the last JSON object/array (in case there's extra text after)
        int lastBrace = cleaned.lastIndexOf('}');
        int lastBracket = cleaned.lastIndexOf(']');
        int jsonEnd = Math.max(lastBrace, lastBracket);
        
        if (jsonEnd > 0 && jsonEnd < cleaned.length() - 1) {
            cleaned = cleaned.substring(0, jsonEnd + 1);
        }
        
        // If the JSON is incomplete, try to complete it
        if (cleaned.startsWith("[") && !cleaned.endsWith("]")) {
            // Try to find the last complete object
            int lastCompleteBrace = cleaned.lastIndexOf("}");
            if (lastCompleteBrace > 0) {
                cleaned = cleaned.substring(0, lastCompleteBrace + 1) + "]";
            }
        }
        
        // If it's an array but should be an object, wrap it
        if (cleaned.startsWith("[") && cleaned.endsWith("]")) {
            cleaned = "{\"anomalies\": " + cleaned + ", \"overallAssessment\": \"Analysis completed\", \"riskLevel\": \"LOW\"}";
        }
        
        logger.debug("Cleaned LLM response: {}", cleaned);
        return cleaned;
    }
}
