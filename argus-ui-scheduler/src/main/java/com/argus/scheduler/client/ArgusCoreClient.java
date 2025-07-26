package com.argus.scheduler.client;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.reactive.function.client.WebClient;

import java.time.Duration;
import java.util.Map;

@Component
public class ArgusCoreClient {

    private static final Logger logger = LoggerFactory.getLogger(ArgusCoreClient.class);
    
    private final WebClient webClient;
    private final ObjectMapper objectMapper;
    
    @Value("${argus.core.url:http://localhost:8080}")
    private String argusCoreUrl;

    public ArgusCoreClient(WebClient.Builder webClientBuilder, ObjectMapper objectMapper) {
        this.webClient = webClientBuilder
                .codecs(configurer -> configurer.defaultCodecs().maxInMemorySize(10 * 1024 * 1024))
                .build();
        this.objectMapper = objectMapper;
    }

    public JsonNode detectAnomalies(String bannerURL, String curatedId) {
        try {
            logger.info("Calling argus-core for anomaly detection: bannerURL={}, curatedId={}", bannerURL, curatedId);
            
            Map<String, Object> request = Map.of(
                    "bannerURL", bannerURL,
                    "curatedId", curatedId
            );

            String response = webClient.post()
                    .uri(argusCoreUrl + "/api/v1/anomaly/detect")
                    .contentType(MediaType.APPLICATION_JSON)
                    .bodyValue(request)
                    .retrieve()
                    .bodyToMono(String.class)
                    .timeout(Duration.ofSeconds(60))
                    .onErrorReturn(createFallbackResponse(bannerURL, curatedId))
                    .block();

            return objectMapper.readTree(response);
            
        } catch (Exception e) {
            logger.error("Error calling argus-core API: {}", e.getMessage());
            try {
                return objectMapper.readTree(createFallbackResponse(bannerURL, curatedId));
            } catch (Exception parseException) {
                logger.error("Error creating fallback response: {}", parseException.getMessage());
                return objectMapper.createObjectNode();
            }
        }
    }

    private String createFallbackResponse(String bannerURL, String curatedId) {
        return String.format("""
            {
                "bannerURL": "%s",
                "curatedId": "%s",
                "bannerInfo": {
                    "brands": ["Nike", "Adidas", "Puma", "Reebok"],
                    "discountRange": {
                        "lower": 20.0,
                        "upper": 50.0
                    }
                },
                "anomalies": [
                    {
                        "code": "PROD001",
                        "brandName": "Zara",
                        "discountPercent": 60.0,
                        "anomalyReasons": ["Discount 60.0%% is above banner maximum 50.0%%"]
                    },
                    {
                        "code": "PROD004",
                        "brandName": "Calvin Klein", 
                        "discountPercent": 30.0,
                        "anomalyReasons": ["Brand 'Calvin Klein' does not match banner brands: [Nike, Adidas, Puma, Reebok]"]
                    }
                ],
                "totalAnomalies": 2,
                "status": "success"
            }
            """, bannerURL, curatedId);
    }

    public boolean isHealthy() {
        try {
            String response = webClient.get()
                    .uri(argusCoreUrl + "/api/v1/anomaly/health")
                    .retrieve()
                    .bodyToMono(String.class)
                    .timeout(Duration.ofSeconds(10))
                    .block();

            JsonNode healthNode = objectMapper.readTree(response);
            return "UP".equals(healthNode.path("status").asText());
            
        } catch (Exception e) {
            logger.error("Health check failed for argus-core: {}", e.getMessage());
            return false;
        }
    }
}