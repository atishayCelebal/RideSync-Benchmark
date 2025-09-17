package com.ridesync.config;

import com.ridesync.service.LLMAnomalyDetectionService;
import com.ridesync.service.impl.OllamaAnomalyDetectionService;
import com.ridesync.service.impl.OpenAIAnomalyDetectionService;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Primary;
import org.springframework.http.client.SimpleClientHttpRequestFactory;
import org.springframework.web.client.RestTemplate;

/**
 * Configuration to choose between different LLM providers
 */
@Configuration
@RequiredArgsConstructor
public class LLMConfig {
    
    @Value("${ridesync.llm.azure.openai.enabled:false}")
    private boolean azureOpenAIEnabled;
    
    @Value("${ridesync.llm.ollama.enabled:true}")
    private boolean ollamaEnabled;
    
    private final OpenAIAnomalyDetectionService azureOpenAIService;
    private final OllamaAnomalyDetectionService ollamaService;
    
    @Bean
    @Primary
    public LLMAnomalyDetectionService llmAnomalyDetectionService() {
        if (azureOpenAIEnabled) {
            System.out.println("🔧 Using Azure OpenAI for LLM analysis");
            return azureOpenAIService;
        } else if (ollamaEnabled) {
            System.out.println("🔧 Using Ollama (FREE) for LLM analysis");
            return ollamaService;
        } else {
            System.out.println("🔧 LLM analysis disabled");
            return azureOpenAIService; // Fallback
        }
    }
    
    @Bean
    public RestTemplate ollamaRestTemplate() {
        SimpleClientHttpRequestFactory factory = new SimpleClientHttpRequestFactory();
        factory.setConnectTimeout(5000); // 5 seconds
        factory.setReadTimeout(15000); // 15 seconds
        return new RestTemplate(factory);
    }
}
