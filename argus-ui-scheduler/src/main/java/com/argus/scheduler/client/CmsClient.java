package com.argus.scheduler.client;

import com.argus.scheduler.model.ComponentInfo;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;
import org.springframework.web.reactive.function.client.WebClient;

import java.time.Duration;
import java.util.ArrayList;
import java.util.List;

@Component
public class CmsClient {

    private static final Logger logger = LoggerFactory.getLogger(CmsClient.class);
    
    private final WebClient webClient;
    private final ObjectMapper objectMapper;

    public CmsClient(WebClient.Builder webClientBuilder, ObjectMapper objectMapper) {
        this.webClient = webClientBuilder
                .codecs(configurer -> configurer.defaultCodecs().maxInMemorySize(10 * 1024 * 1024))
                .build();
        this.objectMapper = objectMapper;
    }

    public List<ComponentInfo> getPageComponents(String cmsUrl) {
        try {
            logger.info("Fetching components from CMS URL: {}", cmsUrl);
            
            String response = webClient.get()
                    .uri(cmsUrl)
                    .header("User-Agent", "Argus-Scheduler-Service/1.0")
                    .retrieve()
                    .bodyToMono(String.class)
                    .timeout(Duration.ofSeconds(30))
                    .onErrorReturn(createFallbackCmsResponse())
                    .block();

            return parseCmsResponse(response);
            
        } catch (Exception e) {
            logger.error("Error fetching components from CMS: {}", e.getMessage());
            return createFallbackComponents();
        }
    }

    private List<ComponentInfo> parseCmsResponse(String response) {
        try {
            JsonNode rootNode = objectMapper.readTree(response);
            List<ComponentInfo> components = new ArrayList<>();
            
            JsonNode componentsNode = rootNode.path("components");
            if (componentsNode.isArray()) {
                for (JsonNode componentNode : componentsNode) {
                    String bannerURL = componentNode.path("bannerURL").asText();
                    String curatedId = componentNode.path("curatedId").asText();
                    String componentType = componentNode.path("type").asText("banner");
                    
                    if (!bannerURL.isEmpty() && !curatedId.isEmpty()) {
                        components.add(new ComponentInfo(bannerURL, curatedId, componentType));
                    }
                }
            }
            
            logger.info("Successfully parsed {} components from CMS", components.size());
            return components;
            
        } catch (Exception e) {
            logger.error("Error parsing CMS response: {}", e.getMessage());
            return createFallbackComponents();
        }
    }

    private String createFallbackCmsResponse() {
        return """
            {
                "components": [
                    {
                        "bannerURL": "https://assets.ajio.com/medias/sys_master/root/20240101/banner1.jpg",
                        "curatedId": "83",
                        "type": "banner"
                    },
                    {
                        "bannerURL": "https://assets.ajio.com/medias/sys_master/root/20240101/banner2.jpg", 
                        "curatedId": "84",
                        "type": "banner"
                    },
                    {
                        "bannerURL": "https://assets.ajio.com/medias/sys_master/root/20240101/banner3.jpg",
                        "curatedId": "85",
                        "type": "banner"
                    }
                ]
            }
            """;
    }

    private List<ComponentInfo> createFallbackComponents() {
        logger.info("Using fallback CMS components");
        
        List<ComponentInfo> components = new ArrayList<>();
        components.add(new ComponentInfo(
                "https://assets.ajio.com/medias/sys_master/root/20240101/banner1.jpg", 
                "83", 
                "banner"
        ));
        components.add(new ComponentInfo(
                "https://assets.ajio.com/medias/sys_master/root/20240101/banner2.jpg", 
                "84", 
                "banner"
        ));
        components.add(new ComponentInfo(
                "https://assets.ajio.com/medias/sys_master/root/20240101/banner3.jpg", 
                "85", 
                "banner"
        ));
        
        return components;
    }
}