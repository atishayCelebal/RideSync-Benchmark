package com.ridesync.service.impl;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.ridesync.service.GroupContextDataCollector;
import com.ridesync.service.LLMAnomalyDetectionService;
import com.ridesync.service.LLMResponseProcessor;
import com.ridesync.service.RateLimitingService;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.*;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

/**
 * Azure OpenAI implementation of LLM anomaly detection service
 */
@Service
@RequiredArgsConstructor
public class OpenAIAnomalyDetectionService implements LLMAnomalyDetectionService {
    
    private static final Logger logger = LoggerFactory.getLogger(OpenAIAnomalyDetectionService.class);
    
    private final RateLimitingService rateLimitingService;
    private final GroupContextDataCollector dataCollector;
    private final LLMResponseProcessor responseProcessor;
    private final ObjectMapper objectMapper;
    private final RestTemplate restTemplate;
    
    @Value("${ridesync.llm.azure.openai.api-key:}")
    private String azureApiKey;
    
    @Value("${ridesync.llm.enabled:true}")
    private boolean llmEnabled;
    
    // Circuit breaker for quota issues
    private volatile boolean quotaExceeded = false;
    private volatile long quotaResetTime = 0;
    
    @Value("${ridesync.llm.azure.openai.deployment-name:}")
    private String deploymentName;
    
    @Value("${ridesync.llm.azure.openai.api-version:2024-02-15-preview}")
    private String apiVersion;
    
    @Value("${ridesync.llm.azure.openai.max-tokens:500}")
    private int maxTokens;
    
    @Value("${ridesync.llm.azure.openai.temperature:0.3}")
    private double temperature;
    
    @Value("${ridesync.llm.azure.openai.endpoint:}")
    private String azureEndpoint;
    
    @Override
    public Map<String, Object> analyzeRideData(UUID rideId, UUID userId) {
        logger.info("Starting LLM analysis for ride: {}, user: {}", rideId, userId);
        
        Map<String, Object> result = new HashMap<>();
        
        try {
            // Check if LLM is enabled
            if (!llmEnabled) {
                logger.info("LLM analysis disabled in configuration");
                result.put("status", "disabled");
                result.put("message", "LLM analysis is disabled");
                return result;
            }
            
            // Check if API key is configured
            if (azureApiKey == null || azureApiKey.trim().isEmpty()) {
                logger.info("Azure OpenAI API key not configured, skipping LLM analysis");
                result.put("status", "no_api_key");
                result.put("message", "Azure OpenAI API key not configured");
                return result;
            }
            
            // Check circuit breaker for quota issues
            if (quotaExceeded) {
                if (System.currentTimeMillis() < quotaResetTime) {
                    long remainingTime = (quotaResetTime - System.currentTimeMillis()) / 1000;
                    logger.info("OpenAI quota exceeded, circuit breaker active for {} more seconds", remainingTime);
                    result.put("status", "quota_exceeded");
                    result.put("message", "OpenAI quota exceeded, retry in " + remainingTime + " seconds");
                    return result;
                } else {
                    // Reset circuit breaker
                    logger.info("Circuit breaker timeout expired, resetting");
                    quotaExceeded = false;
                    quotaResetTime = 0;
                }
            }
            
            // Check rate limiting
            if (!canAnalyze(userId)) {
                long remainingTime = rateLimitingService.getRemainingTime(userId);
                logger.info("LLM analysis rate limited for user: {} ({}ms remaining)", userId, remainingTime);
                result.put("status", "rate_limited");
                result.put("remainingTime", remainingTime);
                return result;
            }
            
            // Collect group context data
            Map<String, Object> contextData = dataCollector.collectGroupContextData(rideId, userId);
            
            // Create LLM prompt
            String prompt = createLLMPrompt(contextData);
            
            // Call OpenAI API
            String llmResponse = callOpenAI(prompt);
            
            // Process response
            if (llmResponse != null && responseProcessor.validateLLMResponse(llmResponse)) {
                result.put("status", "success");
                result.put("llmResponse", llmResponse);
                result.put("contextData", contextData);
                
                // Record successful call for rate limiting
                rateLimitingService.recordCall(userId);
                
                logger.info("LLM analysis completed successfully for user: {}", userId);
            } else {
                result.put("status", "invalid_response");
                result.put("error", "LLM returned invalid response format");
                logger.warn("LLM returned invalid response for user: {}", userId);
            }
            
        } catch (Exception e) {
            logger.error("Error in LLM analysis for user: {} - {}", userId, e.getMessage(), e);
            result.put("status", "error");
            result.put("error", e.getMessage());
        }
        
        return result;
    }
    
    @Override
    public boolean canAnalyze(UUID userId) {
        return rateLimitingService.canMakeCall(userId);
    }
    
    @Override
    public Long getLastAnalysisTime(UUID userId) {
        return rateLimitingService.getLastAnalysisTime(userId);
    }
    
    @Override
    public String[] getSupportedAnomalyTypes() {
        return new String[]{
            "STATIONARY_ANOMALY",
            "SPEED_ANOMALY", 
            "DIRECTION_DRIFT",
            "ROUTE_DEVIATION",
            "LOCATION_ANOMALY",
            "GROUP_COORDINATION",
            "GPS_ANOMALY",
            "EMERGENCY"
        };
    }
    
    /**
     * Create LLM prompt for anomaly detection
     * @param contextData The group context data
     * @return Formatted prompt string
     */
    private String createLLMPrompt(Map<String, Object> contextData) {
        StringBuilder prompt = new StringBuilder();
        
        prompt.append("You are an expert in analyzing ride-sharing and group transportation data. ");
        prompt.append("Analyze the following ride data and identify any anomalies or concerning patterns.\n\n");
        
        prompt.append("Ride Context Data:\n");
        prompt.append("```json\n");
        try {
            prompt.append(objectMapper.writeValueAsString(contextData));
        } catch (Exception e) {
            logger.error("Error formatting context data: {}", e.getMessage());
            prompt.append("Error formatting data");
        }
        prompt.append("\n```\n\n");
        
        prompt.append("Please identify anomalies in the following categories:\n");
        prompt.append("1. Stationary anomalies (unexpected stops or lack of movement)\n");
        prompt.append("2. Speed anomalies (too fast/slow for conditions or group)\n");
        prompt.append("3. Route deviations (off expected path or group direction)\n");
        prompt.append("4. Group coordination issues (members too far apart or moving independently)\n");
        prompt.append("5. Safety concerns (dangerous speeds, erratic behavior)\n");
        prompt.append("6. Technical issues (GPS accuracy problems, data inconsistencies)\n\n");
        
        prompt.append("Provide your analysis in JSON format:\n");
        prompt.append("{\n");
        prompt.append("  \"anomalies\": [\n");
        prompt.append("    {\n");
        prompt.append("      \"type\": \"ANOMALY_TYPE\",\n");
        prompt.append("      \"severity\": \"LOW|MEDIUM|HIGH|CRITICAL\",\n");
        prompt.append("      \"description\": \"Detailed description of the anomaly\",\n");
        prompt.append("      \"userId\": \"user-uuid\",\n");
        prompt.append("      \"timestamp\": \"2025-09-15T08:15:00\",\n");
        prompt.append("      \"confidence\": 0.85,\n");
        prompt.append("      \"recommendations\": [\"Action item 1\", \"Action item 2\"]\n");
        prompt.append("    }\n");
        prompt.append("  ],\n");
        prompt.append("  \"overallAssessment\": \"Summary of the ride situation\",\n");
        prompt.append("  \"riskLevel\": \"LOW|MEDIUM|HIGH|CRITICAL\"\n");
        prompt.append("}\n\n");
        
        prompt.append("Focus on:\n");
        prompt.append("- Group coordination and member proximity\n");
        prompt.append("- Speed consistency within the group\n");
        prompt.append("- Route adherence and direction\n");
        prompt.append("- Safety concerns and emergency situations\n");
        prompt.append("- Data quality and GPS accuracy issues\n");
        
        return prompt.toString();
    }
    
    /**
     * Call OpenAI API with the prompt
     * @param prompt The formatted prompt
     * @return LLM response or null if failed
     */
    private String callOpenAI(String prompt) {
        try {
            // Build Azure OpenAI endpoint URL
            String azureApiUrl = azureEndpoint + "openai/deployments/" + deploymentName + "/chat/completions?api-version=" + apiVersion;
            
            // Prepare request
            Map<String, Object> requestBody = new HashMap<>();
            requestBody.put("max_tokens", maxTokens);
            requestBody.put("temperature", temperature);
            
            Map<String, String> message = new HashMap<>();
            message.put("role", "user");
            message.put("content", prompt);
            requestBody.put("messages", new Object[]{message});
            
            // Set headers for Azure OpenAI
            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_JSON);
            headers.set("api-key", azureApiKey); // Azure uses api-key header instead of Bearer
            
            HttpEntity<Map<String, Object>> request = new HttpEntity<>(requestBody, headers);
            
            // Make API call
            logger.info("Calling Azure OpenAI API with deployment: {}", deploymentName);
            ResponseEntity<Map> response = restTemplate.postForEntity(azureApiUrl, request, Map.class);
                
            if (response.getStatusCode() == HttpStatus.OK && response.getBody() != null) {
                Map<String, Object> responseBody = response.getBody();
                
                // Extract content from response
                if (responseBody.containsKey("choices")) {
                    Object[] choices = (Object[]) responseBody.get("choices");
                    if (choices.length > 0) {
                        Map<String, Object> firstChoice = (Map<String, Object>) choices[0];
                        Map<String, Object> messageObj = (Map<String, Object>) firstChoice.get("message");
                        String content = (String) messageObj.get("content");
                        
                        logger.info("Azure OpenAI API call successful with deployment: {}", deploymentName);
                        return content;
                    }
                }
            }
            
            logger.warn("Azure OpenAI API call failed or returned unexpected format");
            return null;
            
        } catch (Exception e) {
            logger.error("Error calling Azure OpenAI API: {}", e.getMessage());
            
            // If this is a quota/rate limit error, activate circuit breaker
            if (e.getMessage() != null && (e.getMessage().contains("quota") || e.getMessage().contains("429") || e.getMessage().contains("rate"))) {
                logger.warn("Azure OpenAI API quota exceeded or rate limited, activating circuit breaker");
                quotaExceeded = true;
                quotaResetTime = System.currentTimeMillis() + (30 * 60 * 1000); // 30 minutes
                return null;
            }
            
            // For other errors, return null
            return null;
        }
    }
}
