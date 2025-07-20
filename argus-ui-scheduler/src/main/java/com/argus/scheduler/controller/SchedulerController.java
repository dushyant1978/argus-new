package com.argus.scheduler.controller;

import com.argus.scheduler.model.PageAnomalyReport;
import com.argus.scheduler.model.PageConfiguration;
import com.argus.scheduler.service.PageValidationService;
import com.argus.scheduler.service.ReportService;
import com.argus.scheduler.service.SchedulerService;
import jakarta.validation.Valid;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/v1/scheduler")
public class SchedulerController {

    private static final Logger logger = LoggerFactory.getLogger(SchedulerController.class);
    
    @Autowired
    private SchedulerService schedulerService;
    
    @Autowired
    private PageValidationService pageValidationService;
    
    @Autowired
    private ReportService reportService;

    @PostMapping("/run")
    public ResponseEntity<Map<String, String>> runScheduler() {
        logger.info("Manual scheduler run triggered");
        
        try {
            schedulerService.runScan();
            return ResponseEntity.ok(Map.of(
                    "status", "success",
                    "message", "Scheduler run completed successfully"
            ));
            
        } catch (Exception e) {
            logger.error("Error during manual scheduler run: {}", e.getMessage());
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(Map.of(
                    "status", "error",
                    "message", "Scheduler run failed: " + e.getMessage()
            ));
        }
    }

    @PostMapping("/run/{pageName}")
    public ResponseEntity<Map<String, String>> runSchedulerForPage(@PathVariable String pageName) {
        logger.info("Manual scheduler run triggered for page: {}", pageName);
        
        try {
            schedulerService.runScanForPage(pageName);
            return ResponseEntity.ok(Map.of(
                    "status", "success",
                    "message", "Scheduler run completed successfully for page: " + pageName
            ));
            
        } catch (Exception e) {
            logger.error("Error during manual scheduler run for page {}: {}", pageName, e.getMessage());
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(Map.of(
                    "status", "error",
                    "message", "Scheduler run failed for page " + pageName + ": " + e.getMessage()
            ));
        }
    }

    @GetMapping("/reports")
    public ResponseEntity<List<PageAnomalyReport>> getReports() {
        try {
            List<PageAnomalyReport> reports = reportService.getAllReports();
            return ResponseEntity.ok(reports);
            
        } catch (Exception e) {
            logger.error("Error fetching reports: {}", e.getMessage());
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }

    @GetMapping("/reports/{reportId}/details")
    public ResponseEntity<List<Map<String, Object>>> getReportDetails(@PathVariable Long reportId) {
        try {
            List<Map<String, Object>> details = reportService.getReportDetails(reportId);
            return ResponseEntity.ok(details);
            
        } catch (Exception e) {
            logger.error("Error fetching report details for {}: {}", reportId, e.getMessage());
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }

    @GetMapping("/reports/page/{pageName}")
    public ResponseEntity<List<PageAnomalyReport>> getReportsForPage(@PathVariable String pageName) {
        try {
            List<PageAnomalyReport> reports = reportService.getReportsForPage(pageName);
            return ResponseEntity.ok(reports);
            
        } catch (Exception e) {
            logger.error("Error fetching reports for page {}: {}", pageName, e.getMessage());
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }

    @PostMapping("/pages")
    public ResponseEntity<PageConfiguration> createPage(@Valid @RequestBody CreatePageRequest request) {
        logger.info("Creating new page: {}", request.getPageName());
        
        try {
            PageConfiguration page = pageValidationService.createPage(request.getPageName(), request.getCmsUrl());
            return ResponseEntity.status(HttpStatus.CREATED).body(page);
            
        } catch (IllegalArgumentException e) {
            logger.error("Error creating page: {}", e.getMessage());
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).build();
            
        } catch (Exception e) {
            logger.error("Unexpected error creating page: {}", e.getMessage());
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }

    @GetMapping("/pages")
    public ResponseEntity<List<PageConfiguration>> getPages() {
        try {
            List<PageConfiguration> pages = pageValidationService.getAllPages();
            return ResponseEntity.ok(pages);
            
        } catch (Exception e) {
            logger.error("Error fetching pages: {}", e.getMessage());
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }

    @PutMapping("/pages/{id}")
    public ResponseEntity<PageConfiguration> updatePage(
            @PathVariable Long id, 
            @Valid @RequestBody UpdatePageRequest request) {
        
        logger.info("Updating page with id: {}", id);
        
        try {
            PageConfiguration page = pageValidationService.updatePage(
                    id, request.getPageName(), request.getCmsUrl(), request.getActive());
            return ResponseEntity.ok(page);
            
        } catch (IllegalArgumentException e) {
            logger.error("Error updating page: {}", e.getMessage());
            return ResponseEntity.status(HttpStatus.NOT_FOUND).build();
            
        } catch (Exception e) {
            logger.error("Unexpected error updating page: {}", e.getMessage());
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }

    @DeleteMapping("/pages/{id}")
    public ResponseEntity<Void> deletePage(@PathVariable Long id) {
        logger.info("Deleting page with id: {}", id);
        
        try {
            pageValidationService.deletePage(id);
            return ResponseEntity.noContent().build();
            
        } catch (IllegalArgumentException e) {
            logger.error("Error deleting page: {}", e.getMessage());
            return ResponseEntity.status(HttpStatus.NOT_FOUND).build();
            
        } catch (Exception e) {
            logger.error("Unexpected error deleting page: {}", e.getMessage());
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }

    @PostMapping("/pages/{id}/toggle")
    public ResponseEntity<Map<String, String>> togglePageStatus(@PathVariable Long id) {
        logger.info("Toggling status for page with id: {}", id);
        
        try {
            pageValidationService.togglePageStatus(id);
            return ResponseEntity.ok(Map.of(
                    "status", "success",
                    "message", "Page status toggled successfully"
            ));
            
        } catch (IllegalArgumentException e) {
            logger.error("Error toggling page status: {}", e.getMessage());
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body(Map.of(
                    "status", "error",
                    "message", e.getMessage()
            ));
            
        } catch (Exception e) {
            logger.error("Unexpected error toggling page status: {}", e.getMessage());
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(Map.of(
                    "status", "error",
                    "message", "Unexpected error: " + e.getMessage()
            ));
        }
    }

    public static class CreatePageRequest {
        private String pageName;
        private String cmsUrl;

        public String getPageName() { return pageName; }
        public void setPageName(String pageName) { this.pageName = pageName; }
        public String getCmsUrl() { return cmsUrl; }
        public void setCmsUrl(String cmsUrl) { this.cmsUrl = cmsUrl; }
    }

    public static class UpdatePageRequest {
        private String pageName;
        private String cmsUrl;
        private Boolean active;

        public String getPageName() { return pageName; }
        public void setPageName(String pageName) { this.pageName = pageName; }
        public String getCmsUrl() { return cmsUrl; }
        public void setCmsUrl(String cmsUrl) { this.cmsUrl = cmsUrl; }
        public Boolean getActive() { return active; }
        public void setActive(Boolean active) { this.active = active; }
    }
}