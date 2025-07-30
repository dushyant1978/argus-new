package com.argus.scheduler.service;

import com.argus.scheduler.client.ArgusCoreClient;
import com.argus.scheduler.client.CmsClient;
import com.argus.scheduler.model.ComponentInfo;
import com.argus.scheduler.model.PageAnomalyReport;
import com.argus.scheduler.model.PageConfiguration;
import com.argus.scheduler.repository.PageConfigurationRepository;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Service
public class SchedulerService {

    private static final Logger logger = LoggerFactory.getLogger(SchedulerService.class);
    
    @Autowired
    private PageConfigurationRepository pageConfigRepository;
    
    @Autowired
    private CmsClient cmsClient;
    
    @Autowired
    private ArgusCoreClient argusCoreClient;
    
    @Autowired
    private ReportService reportService;
    
    @Autowired
    private ObjectMapper objectMapper;

    @Scheduled(cron = "${argus.scheduler.cron:0 0 */4 * * *}") // Every 4 hours by default
    public void scheduledScan() {
        logger.info("Starting scheduled anomaly scan");
        runScan();
    }

    public void runScan() {
        List<PageConfiguration> activePages = pageConfigRepository.findByActiveTrue();
        logger.info("Found {} active pages to scan", activePages.size());
        
        for (PageConfiguration page : activePages) {
            try {
                scanPage(page);
            } catch (Exception e) {
                logger.error("Error scanning page {}: {}", page.getPageName(), e.getMessage());
            }
        }
        
        logger.info("Completed scheduled anomaly scan");
    }

    private void scanPage(PageConfiguration page) {
        logger.info("Scanning page: {} ({})", page.getPageName(), page.getPageId());
        
        try {
            // Get components from CMS
            List<ComponentInfo> components = cmsClient.getPageComponents(page.getPageId());
            
            if (components.isEmpty()) {
                logger.warn("No components found for page: {}", page.getPageName());
                return;
            }
            
            // Scan each component for anomalies
            List<Map<String, Object>> componentReports = new ArrayList<>();
            int totalAnomalies = 0;
            int componentsWithAnomalies = 0;
            
            for (ComponentInfo component : components) {
                try {
                    JsonNode anomalyResult = argusCoreClient.detectAnomalies(
                            component.getBannerURL(), 
                            component.getCuratedId()
                    );
                    
                    Map<String, Object> componentReport = new HashMap<>();
                    componentReport.put("bannerURL", component.getBannerURL());
                    componentReport.put("curatedId", component.getCuratedId());
                    componentReport.put("componentType", component.getComponentType());
                    componentReport.put("anomalyResult", anomalyResult);
                    
                    int componentAnomalies = anomalyResult.path("totalAnomalies").asInt(0);
                    totalAnomalies += componentAnomalies;
                    
                    if (componentAnomalies > 0) {
                        componentsWithAnomalies++;
                    }
                    
                    componentReports.add(componentReport);
                    
                } catch (Exception e) {
                    logger.error("Error scanning component {}: {}", component.getBannerURL(), e.getMessage());
                    
                    Map<String, Object> errorReport = new HashMap<>();
                    errorReport.put("bannerURL", component.getBannerURL());
                    errorReport.put("curatedId", component.getCuratedId());
                    errorReport.put("error", e.getMessage());
                    componentReports.add(errorReport);
                }
            }
            
            // Create and save report
            String reportDataJson = objectMapper.writeValueAsString(componentReports);
            
            PageAnomalyReport report = new PageAnomalyReport(
                    page.getPageName(),
                    page.getPageId(),
                    components.size(),
                    componentsWithAnomalies,
                    totalAnomalies,
                    reportDataJson,
                    "completed"
            );
            
            reportService.saveReport(report);
            
            logger.info("Completed scanning page {}: {} components, {} with anomalies, {} total anomalies",
                       page.getPageName(), components.size(), componentsWithAnomalies, totalAnomalies);
            
        } catch (Exception e) {
            logger.error("Error during page scan for {}: {}", page.getPageName(), e.getMessage());
            
            // Save error report
            PageAnomalyReport errorReport = new PageAnomalyReport(
                    page.getPageName(),
                    page.getPageId(),
                    0,
                    0,
                    0,
                    "{\"error\": \"" + e.getMessage() + "\"}",
                    "error"
            );
            
            reportService.saveReport(errorReport);
        }
    }

    public void runScanForPage(String pageName) {
        PageConfiguration page = pageConfigRepository.findByActiveTrue().stream()
                .filter(p -> p.getPageName().equals(pageName))
                .findFirst()
                .orElseThrow(() -> new RuntimeException("Page not found: " + pageName));
        
        scanPage(page);
    }
}