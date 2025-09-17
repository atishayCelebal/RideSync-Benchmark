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
 * Ollama implementation of LLM anomaly detection service (FREE)
 */
@Service
@RequiredArgsConstructor
public class OllamaAnomalyDetectionService implements LLMAnomalyDetectionService {
    
    private static final Logger logger = LoggerFactory.getLogger(OllamaAnomalyDetectionService.class);
    
    private final RateLimitingService rateLimitingService;
    private final GroupContextDataCollector dataCollector;
    private final LLMResponseProcessor responseProcessor;
    private final ObjectMapper objectMapper;
    private final RestTemplate restTemplate;
    
    @Value("${ridesync.llm.enabled:true}")
    private boolean llmEnabled;
    
    @Value("${ridesync.llm.ollama.enabled:true}")
    private boolean ollamaEnabled;
    
    @Value("${ridesync.llm.ollama.endpoint:http://localhost:11434}")
    private String ollamaEndpoint;
    
    @Value("${ridesync.llm.ollama.model:llama2}")
    private String ollamaModel;
    
    @Value("${ridesync.llm.ollama.timeout:30000}")
    private int timeoutMs;
    
    @Value("${ridesync.llm.ollama.max-tokens:500}")
    private int maxTokens;
    
    @Value("${ridesync.llm.ollama.temperature:0.3}")
    private double temperature;
    
    // Circuit breaker for quota issues
    private volatile boolean quotaExceeded = false;
    private volatile long quotaResetTime = 0;
    
    @Override
    public Map<String, Object> analyzeRideData(UUID rideId, UUID userId) {
        logger.info("Starting Ollama LLM analysis for ride: {}, user: {}", rideId, userId);
        
        Map<String, Object> result = new HashMap<>();
        
        try {
            // Check if LLM is enabled
            if (!llmEnabled || !ollamaEnabled) {
                logger.info("Ollama LLM analysis disabled in configuration");
                result.put("status", "disabled");
                result.put("message", "Ollama LLM analysis is disabled");
                return result;
            }
            
            // Check circuit breaker for quota issues
            if (quotaExceeded) {
                if (System.currentTimeMillis() < quotaResetTime) {
                    long remainingTime = (quotaResetTime - System.currentTimeMillis()) / 1000;
                    logger.info("Ollama quota exceeded, circuit breaker active for {} more seconds", remainingTime);
                    result.put("status", "quota_exceeded");
                    result.put("message", "Ollama quota exceeded, retry in " + remainingTime + " seconds");
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
                logger.info("Ollama LLM analysis rate limited for user: {} ({}ms remaining)", userId, remainingTime);
                result.put("status", "rate_limited");
                result.put("remainingTime", remainingTime);
                return result;
            }
            
            // Collect group context data
            Map<String, Object> contextData = dataCollector.collectGroupContextData(rideId, userId);
            
            // Create LLM prompt
            String prompt = createLLMPrompt(contextData);
            
            // Call Ollama API
            String llmResponse = callOllama(prompt);
            
            // Process response
            if (llmResponse != null && responseProcessor.validateLLMResponse(llmResponse)) {
                result.put("status", "success");
                result.put("llmResponse", llmResponse);
                result.put("contextData", contextData);
                
                // Record successful call for rate limiting
                rateLimitingService.recordCall(userId);
                
                logger.info("Ollama LLM analysis completed successfully for user: {}", userId);
            } else {
                result.put("status", "invalid_response");
                result.put("error", "Ollama returned invalid response format");
                logger.warn("Ollama returned invalid response for user: {}", userId);
            }
            
        } catch (Exception e) {
            logger.error("Error in Ollama LLM analysis for user: {} - {}", userId, e.getMessage(), e);
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
        
        prompt.append("Respond with this exact JSON format:\n");
        prompt.append("{\"anomalies\": [], \"overallAssessment\": \"No issues\", \"riskLevel\": \"LOW\"}\n\n");
        
        prompt.append("Focus on:\n");
        prompt.append("- Group coordination and member proximity\n");
        prompt.append("- Speed consistency within the group\n");
        prompt.append("- Route adherence and direction\n");
        prompt.append("- Safety concerns and emergency situations\n");
        prompt.append("- Data quality and GPS accuracy issues\n\n");
        
        prompt.append("CRITICAL: Your response must be complete JSON. If no anomalies are found, return: {\"anomalies\": [], \"overallAssessment\": \"No anomalies detected\", \"riskLevel\": \"LOW\"}\n");
        prompt.append("Do not truncate your response. Complete the entire JSON structure.\n");
        
        return prompt.toString();
    }
    
    /**
     * Call Ollama API with the prompt
     */
    private String callOllama(String prompt) {
        try {
            // Build Ollama endpoint URL
            String ollamaApiUrl = ollamaEndpoint + "/api/generate";
            
            // Use the full detailed prompt for better results
            Map<String, Object> requestBody = new HashMap<>();
            requestBody.put("model", ollamaModel);
            requestBody.put("prompt", prompt);
            requestBody.put("stream", false);
            requestBody.put("options", Map.of(
                "temperature", temperature,
                "num_predict", maxTokens // Use full token limit for complete responses
            ));
            
            // Set headers
            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_JSON);
            
            HttpEntity<Map<String, Object>> request = new HttpEntity<>(requestBody, headers);
            
            // Make API call with timeout
            logger.info("Calling Ollama API with model: {} (timeout: {}ms)", ollamaModel, timeoutMs);
            
            // Timeout is handled by the RestTemplate bean configuration
            
            ResponseEntity<Map> response = restTemplate.postForEntity(ollamaApiUrl, request, Map.class);
            
            if (response.getStatusCode() == HttpStatus.OK && response.getBody() != null) {
                Map<String, Object> responseBody = response.getBody();
                
                // Extract content from response
                if (responseBody.containsKey("response")) {
                    String content = (String) responseBody.get("response");
                    
                    logger.info("Ollama API call successful with model: {}", ollamaModel);
                    return content;
                }
            }
            
            logger.warn("Ollama API call failed or returned unexpected format");
            return null;
            
        } catch (Exception e) {
            logger.error("Error calling Ollama API: {}", e.getMessage());
            
            // If this is a quota/rate limit error, activate circuit breaker
            if (e.getMessage() != null && (e.getMessage().contains("quota") || e.getMessage().contains("429") || e.getMessage().contains("rate"))) {
                logger.warn("Ollama API quota exceeded or rate limited, activating circuit breaker");
                quotaExceeded = true;
                quotaResetTime = System.currentTimeMillis() + (30 * 60 * 1000); // 30 minutes
                return null;
            }
            
            // For other errors, return null
            return null;
        }
    }
}
