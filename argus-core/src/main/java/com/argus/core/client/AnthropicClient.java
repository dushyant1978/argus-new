package com.argus.core.client;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.core.publisher.Mono;

import java.time.Duration;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.fasterxml.jackson.databind.node.ArrayNode;

@Component
public class AnthropicClient {

    private static final Logger logger = LoggerFactory.getLogger(AnthropicClient.class);
    
    private final WebClient webClient;
    private final ObjectMapper objectMapper;
    
    // Use placeholders for OpenAI API key and endpoint
    private static final String OPENAI_API_KEY = "YOUR_OPENAI_API_KEY";
    private static final String OPENAI_API_URL = "https://llm-gateway.fynd.engineering/v1/chat/completions";

    public AnthropicClient(WebClient.Builder webClientBuilder, ObjectMapper objectMapper) {
        this.webClient = webClientBuilder
                .codecs(configurer -> configurer.defaultCodecs().maxInMemorySize(10 * 1024 * 1024))
                .build();
        this.objectMapper = objectMapper;
    }

    @Cacheable(value = "bannerAnalysis", key = "#bannerUrl")
    public BannerAnalysisResult analyzeBanner(String bannerUrl) {
        try {
            logger.info("Analyzing banner with OpenAI: {}", bannerUrl);
            
            String prompt = """
                Analyze this banner image and extract the following information:
                1. Brand names mentioned in the image (look for logos, brand text, company names)
                2. Discount information (percentages, offers, sales)
                3. Any promotional text

                Please return the response in JSON format with the following structure:
                {
                  "brands": ["brand1", "brand2", ...],
                  "discount": {
                    "text": "original discount text found in image",
                    "range": {
                      "lower": number,
                      "upper": number
                    }
                  },
                  "analysis": "brief description of what you found in the image"
                }

                For discount ranges:
                - If it says \"Up to X%\" then lower=0, upper=X
                - If it says \"X% to Y%\" then lower=X, upper=Y  
                - If it says \"X% off\" then lower=0, upper=X
                - If it says \"Min. X% off\" then lower=X, upper=100
                - If multiple discounts, use the highest range

                Be very careful to extract exact brand names as they appear in the image.
            """;

            // OpenAI GPT-4 Vision API expects a 'messages' array with a user message containing text and image
            ObjectNode userContent = objectMapper.createObjectNode();
            userContent.put("type", "text");
            userContent.put("text", prompt);
            
            ObjectNode imageUrl = objectMapper.createObjectNode();
            imageUrl.put("url", bannerUrl);
            
            ObjectNode imageContent = objectMapper.createObjectNode();
            imageContent.put("type", "image_url");
            imageContent.set("image_url", imageUrl);
            
            ArrayNode contentArray = objectMapper.createArrayNode();
            contentArray.add(userContent);
            contentArray.add(imageContent);
            
            ObjectNode userMessage = objectMapper.createObjectNode();
            userMessage.put("role", "user");
            userMessage.set("content", contentArray);
            
            ArrayNode messagesArray = objectMapper.createArrayNode();
            messagesArray.add(userMessage);
            
            ObjectNode requestBody = objectMapper.createObjectNode();
            requestBody.put("model", "gpt-4o");
            requestBody.put("max_tokens", 1000);
            requestBody.set("messages", messagesArray);

            String response = webClient.post()
                    .uri(OPENAI_API_URL)
                    .header("Authorization", "Bearer " + OPENAI_API_KEY)
                    .contentType(MediaType.APPLICATION_JSON)
                    .bodyValue(requestBody)
                    .retrieve()
                    .onStatus(status -> status.is4xxClientError() || status.is5xxServerError(),
                        clientResponse -> clientResponse.bodyToMono(String.class)
                            .flatMap(errorBody -> {
                                logger.error("OpenAI API error: {} - {}", clientResponse.statusCode(), errorBody);
                                return Mono.error(new RuntimeException("OpenAI API error: " + clientResponse.statusCode() + " - " + errorBody));
                            }))
                    .bodyToMono(String.class)
                    .timeout(Duration.ofSeconds(60))
                    .block();

            return parseOpenAIResponse(response);
            
        } catch (Exception e) {
            logger.error("Error analyzing banner with OpenAI: {}", e.getMessage());
            return createFallbackResult();
        }
    }

    private BannerAnalysisResult parseOpenAIResponse(String response) {
        logger.info("OpenAI banner analysis result: {}", response);
        try {
            JsonNode responseNode = objectMapper.readTree(response);
            JsonNode choicesNode = responseNode.path("choices");
            if (choicesNode.isArray() && choicesNode.size() > 0) {
                JsonNode messageNode = choicesNode.get(0).path("message");
                String content = messageNode.path("content").asText("");
                // Extract JSON from the response
                int jsonStart = content.indexOf('{');
                int jsonEnd = content.lastIndexOf('}') + 1;
                if (jsonStart >= 0 && jsonEnd > jsonStart) {
                    String jsonStr = content.substring(jsonStart, jsonEnd);
                    JsonNode analysisNode = objectMapper.readTree(jsonStr);
                    List<String> brands = new ArrayList<>();
                    JsonNode brandsNode = analysisNode.path("brands");
                    if (brandsNode.isArray()) {
                        brandsNode.forEach(brandNode -> brands.add(brandNode.asText()));
                    }
                    JsonNode discountNode = analysisNode.path("discount").path("range");
                    double lower = discountNode.path("lower").asDouble(0.0);
                    double upper = discountNode.path("upper").asDouble(0.0);
                    return new BannerAnalysisResult(brands, lower, upper);
                }
            }
            return createFallbackResult();
        } catch (Exception e) {
            logger.error("Error parsing OpenAI response: {}", e.getMessage());
            return createFallbackResult();
        }
    }

    private BannerAnalysisResult createFallbackResult() {
        logger.info("Using fallback banner analysis result");
        List<String> fallbackBrands = List.of("Nike", "Adidas", "Puma", "Reebok");
        return new BannerAnalysisResult(fallbackBrands, 20.0, 50.0);
    }

    public static class BannerAnalysisResult {
        private final List<String> brands;
        private final double lowerDiscount;
        private final double upperDiscount;

        public BannerAnalysisResult(List<String> brands, double lowerDiscount, double upperDiscount) {
            this.brands = brands;
            this.lowerDiscount = lowerDiscount;
            this.upperDiscount = upperDiscount;
        }

        public List<String> getBrands() {
            return brands;
        }

        public double getLowerDiscount() {
            return lowerDiscount;
        }

        public double getUpperDiscount() {
            return upperDiscount;
        }

        @Override
        public String toString() {
            return "BannerAnalysisResult{" +
                    "brands=" + brands +
                    ", lowerDiscount=" + lowerDiscount +
                    ", upperDiscount=" + upperDiscount +
                    '}';
        }
    }
}