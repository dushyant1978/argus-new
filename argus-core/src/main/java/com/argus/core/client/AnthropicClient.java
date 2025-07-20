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

@Component
public class AnthropicClient {

    private static final Logger logger = LoggerFactory.getLogger(AnthropicClient.class);
    
    private final WebClient webClient;
    private final ObjectMapper objectMapper;
    
    @Value("${anthropic.api.key}")
    private String apiKey;
    
    @Value("${anthropic.api.url:https://api.anthropic.com}")
    private String apiUrl;

    public AnthropicClient(WebClient.Builder webClientBuilder, ObjectMapper objectMapper) {
        this.webClient = webClientBuilder
                .codecs(configurer -> configurer.defaultCodecs().maxInMemorySize(10 * 1024 * 1024))
                .build();
        this.objectMapper = objectMapper;
    }

    //@Cacheable(value = "bannerAnalysis", key = "#bannerUrl")
    public BannerAnalysisResult analyzeBanner(String bannerUrl) {
        try {
            logger.info("Analyzing banner: {}", bannerUrl);
            
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
				- If it says "Up to X%" then lower=0, upper=X
				- If it says "X% to Y%" then lower=X, upper=Y  
				- If it says "X% off" then lower=0, upper=X
				- If it says "Min. X% off" then lower=X, upper=100
				- If multiple discounts, use the highest range

				Be very careful to extract exact brand names as they appear in the image.
                """;

            Map<String, Object> requestBody = Map.of(
                "model", "claude-sonnet-4-20250514",
                "max_tokens", 1000,
                "messages", List.of(
                    Map.of(
                        "role", "user",
                        "content", List.of(
                            Map.of("type", "text", "text", prompt),
                            Map.of("type", "image", "source", Map.of(
                                "type", "url",
                                "url", bannerUrl
                            ))
                        )
                    )
                )
            );

            String response = webClient.post()
                    .uri(apiUrl + "/v1/messages")
                    .header("x-api-key", apiKey)
                    .header("anthropic-version", "2023-06-01")
                    .contentType(MediaType.APPLICATION_JSON)
                    .bodyValue(requestBody)
                    .retrieve()
                    .bodyToMono(String.class)
                    .timeout(Duration.ofSeconds(30))
                    .block();

            return parseAnthropicResponse(response);
            
        } catch (Exception e) {
            logger.error("Error analyzing banner with Anthropic: {}", e.getMessage());
            return createFallbackResult();
        }
    }

    private BannerAnalysisResult parseAnthropicResponse(String response) {
		
		logger.info("banner analysis result: {}", response);
		
//banner analysis result: {"id":"msg_01MhtTtnyYKrdBfpYcrNAc1g","type":"message","role":"assistant","model":"claude-sonnet-4-20250514","content":[{"type":"text","text":"```json\n{\n  \"brands\": [\"THE BEAR HOUSE\", \"CANTABIL\"],\n  \"discount\": {\n    \"text\": \"MIN. 60% OFF\",\n    \"range\": {\n      \"lower\": 60,\n      \"upper\": 100\n    }\n  },\n  \"analysis\": \"This is a fashion banner featuring smart shirts and t-shirts with two male models in a tropical setting. The image prominently displays 'THE BEAR HOUSE' and 'CANTABIL' brand logos, along with '& more' indicating additional brands. The main offer is 'MIN. 60% OFF' with a 'SHOP NOW' call-to-action button.\"\n}\n```"}],"stop_reason":"end_turn","stop_sequence":null,"usage":{"input_tokens":1043,"cache_creation_input_tokens":0,"cache_read_input_tokens":0,"output_tokens":162,"service_tier":"standard"}}

		
		
        try {
            JsonNode responseNode = objectMapper.readTree(response);
            JsonNode contentNode = responseNode.path("content");
            
            if (contentNode.isArray() && contentNode.size() > 0) {
                String analysisText = contentNode.get(0).path("text").asText();
                
                // Extract JSON from the response
                int jsonStart = analysisText.indexOf('{');
                int jsonEnd = analysisText.lastIndexOf('}') + 1;
                
                if (jsonStart >= 0 && jsonEnd > jsonStart) {
                    String jsonStr = analysisText.substring(jsonStart, jsonEnd);
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
            logger.error("Error parsing Anthropic response: {}", e.getMessage());
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