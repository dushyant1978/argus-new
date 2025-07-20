package com.argus.core.service;

import com.argus.core.model.*;
import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

@Service
public class AnomalyDetectionService {

    private static final Logger logger = LoggerFactory.getLogger(AnomalyDetectionService.class);
    
    @Autowired
    private BannerAnalysisService bannerAnalysisService;
    
    @Autowired
    private ProductService productService;

    public AnomalyResponse detectAnomalies(AnomalyRequest request) {
        logger.info("Starting anomaly detection for: {}", request);
        
        try {
            // Step 1: Analyze banner
            BannerInfo bannerInfo = bannerAnalysisService.analyzeBanner(request.getBannerURL());
            
            // Step 2: Fetch products
            List<Product> products = productService.getProducts(request.getCuratedId());
            
            // Step 3: Detect anomalies
            List<AnomalyResponse.ProductAnomaly> anomalies = detectProductAnomalies(products, bannerInfo);
            
            // Step 4: Limit to first 50 anomalies
            List<AnomalyResponse.ProductAnomaly> limitedAnomalies = anomalies.stream()
                    .limit(50)
                    .collect(Collectors.toList());
            
            logger.info("Found {} total anomalies, returning first {}", anomalies.size(), limitedAnomalies.size());
            
            return new AnomalyResponse(request.getBannerURL(), request.getCuratedId(), bannerInfo, limitedAnomalies);
            
        } catch (Exception e) {
            logger.error("Error during anomaly detection: {}", e.getMessage(), e);
            
            AnomalyResponse errorResponse = new AnomalyResponse();
            errorResponse.setBannerURL(request.getBannerURL());
            errorResponse.setCuratedId(request.getCuratedId());
            errorResponse.setStatus("error");
            errorResponse.setMessage("Failed to detect anomalies: " + e.getMessage());
            errorResponse.setAnomalies(new ArrayList<>());
            
            return errorResponse;
        }
    }

    private List<AnomalyResponse.ProductAnomaly> detectProductAnomalies(List<Product> products, BannerInfo bannerInfo) {
        List<AnomalyResponse.ProductAnomaly> anomalies = new ArrayList<>();
        
        for (Product product : products) {
            List<String> anomalyReasons = new ArrayList<>();
            
            // Check discount anomalies
            if (product.getDiscountPercent() != null && bannerInfo.getDiscountRange() != null) {
                Double discount = product.getDiscountPercent();
                BannerInfo.DiscountRange range = bannerInfo.getDiscountRange();
                
                if (discount < range.getLower()) {
                    anomalyReasons.add("Discount " + discount + "% is below banner minimum " + range.getLower() + "%");
                } else if (discount > range.getUpper()) {
                    anomalyReasons.add("Discount " + discount + "% is above banner maximum " + range.getUpper() + "%");
                }
            }
            
            // Check brand anomalies
            if (product.getFnlColorVariantData() != null && 
                StringUtils.isNotBlank(product.getFnlColorVariantData().getBrandName()) &&
                bannerInfo.getBrands() != null && !bannerInfo.getBrands().isEmpty()) {
                
                String productBrand = product.getFnlColorVariantData().getBrandName();
                boolean brandMatches = bannerInfo.getBrands().stream()
                        .anyMatch(bannerBrand -> 
                            StringUtils.containsIgnoreCase(productBrand, bannerBrand) ||
                            StringUtils.containsIgnoreCase(bannerBrand, productBrand)
                        );
                
                if (!brandMatches) {
                    anomalyReasons.add("Brand '" + productBrand + "' does not match banner brands: " + bannerInfo.getBrands());
                }
            }
            
            // If anomalies found, add to list
            if (!anomalyReasons.isEmpty()) {
                String brandName = product.getFnlColorVariantData() != null ? 
                        product.getFnlColorVariantData().getBrandName() : "Unknown";
                        
                AnomalyResponse.ProductAnomaly anomaly = new AnomalyResponse.ProductAnomaly(
                        product.getCode(),
                        brandName,
                        product.getDiscountPercent(),
                        anomalyReasons
                );
                
                anomalies.add(anomaly);
            }
        }
        
        logger.info("Detected {} anomalies out of {} products", anomalies.size(), products.size());
        
        return anomalies;
    }
}