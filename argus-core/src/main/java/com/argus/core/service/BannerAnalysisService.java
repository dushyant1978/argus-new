package com.argus.core.service;

import com.argus.core.client.OpenAIClient;
import com.argus.core.model.BannerInfo;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

@Service
public class BannerAnalysisService {

    private static final Logger logger = LoggerFactory.getLogger(BannerAnalysisService.class);
    
    @Autowired
    private OpenAIClient openAIClient;

    public BannerInfo analyzeBanner(String bannerUrl) {
        logger.info("Starting banner analysis for URL: {}", bannerUrl);
        
        OpenAIClient.BannerAnalysisResult result = openAIClient.analyzeBanner(bannerUrl);
        
        BannerInfo.DiscountRange discountRange = new BannerInfo.DiscountRange(
                result.getLowerDiscount(), 
                result.getUpperDiscount()
        );
        
        BannerInfo bannerInfo = new BannerInfo(result.getBrands(), discountRange);
        
        logger.info("Banner analysis completed: {}", bannerInfo);
        
        return bannerInfo;
    }
}