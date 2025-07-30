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

    public List<ComponentInfo> getPageComponents(String pageId) {
        try {
            logger.info("Fetching components from CMS for pageId: {}", pageId);
            
            // Create the request body for CMS API
            String requestBody = createCmsRequestBody(pageId);
            
            String response = webClient.post()
                    .uri("https://cms-edge.services.ajio.com/storefront/cms/page")
                    .header("ad_id", "4ae84593-ce0b-4dbf-933a-b43b481628af")
                    .header("userCohortValues", "")
                    .header("Accept-Charset", "UTF-8")
                    .header("Content-Type", "application/json")
                    .header("Accept", "application/json")
                    .header("User-Agent", "Android")
                    .header("client_type", "Android")
                    .header("client_version", "9.23000.0")
                    .bodyValue(requestBody)
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

    private String createCmsRequestBody(String pageId) {
        return String.format("""
            {
                "appVersion": "9.23000.0",
                "userStatus": "NON_LOGGED_IN",
                "apiVersion": "v3",
                "experiments": [
                    "CMSABExp4",
                    "CMSABExp5",
                    "CMSABExp6",
                    "CMSABExp7",
                    "CMSABExp8",
                    "CMSABExp10"
                ],
                "channel": "Android",
                "tenantId": "AJIO",
                "pageUrl": "%s",
                "store": "AJIO",
                "platform": "MOBILE"
            }
            """, pageId);
    }

    private List<ComponentInfo> parseCmsResponse(String response) {
       logger.info("Parsing CMS response: {}", response);
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