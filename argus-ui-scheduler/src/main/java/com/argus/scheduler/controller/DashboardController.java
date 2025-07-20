package com.argus.scheduler.controller;

import com.argus.scheduler.client.ArgusCoreClient;
import com.argus.scheduler.model.PageAnomalyReport;
import com.argus.scheduler.model.PageConfiguration;
import com.argus.scheduler.service.PageValidationService;
import com.argus.scheduler.service.ReportService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestParam;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

@Controller
public class DashboardController {

    @Autowired
    private ReportService reportService;
    
    @Autowired
    private PageValidationService pageValidationService;
    
    @Autowired
    private ArgusCoreClient argusCoreClient;

    @GetMapping("/")
    public String dashboard(Model model) {
        try {
            // Get recent reports (last 24 hours)
            LocalDateTime yesterday = LocalDateTime.now().minusHours(24);
            List<PageAnomalyReport> recentReports = reportService.getRecentReports(yesterday);
            
            // Get all page configurations
            List<PageConfiguration> pages = pageValidationService.getAllPages();
            
            // Get distinct page names from reports
            List<String> pageNames = reportService.getDistinctPageNames();
            
            // Calculate summary statistics
            long totalReports = reportService.getTotalReportsCount();
            long totalAnomalies = reportService.getTotalAnomaliesAcrossAllReports();
            
            // Check argus-core health
            boolean coreServiceHealthy = argusCoreClient.isHealthy();
            
            model.addAttribute("recentReports", recentReports);
            model.addAttribute("pages", pages);
            model.addAttribute("pageNames", pageNames);
            model.addAttribute("totalReports", totalReports);
            model.addAttribute("totalAnomalies", totalAnomalies);
            model.addAttribute("coreServiceHealthy", coreServiceHealthy);
            model.addAttribute("recentReportsCount", recentReports.size());
            
            return "dashboard";
            
        } catch (Exception e) {
            model.addAttribute("error", "Error loading dashboard: " + e.getMessage());
            return "error";
        }
    }

    @GetMapping("/reports")
    public String reportsPage(Model model, @RequestParam(required = false) String page) {
        try {
            List<PageAnomalyReport> reports;
            
            if (page != null && !page.isEmpty()) {
                reports = reportService.getReportsForPage(page);
                model.addAttribute("selectedPage", page);
            } else {
                reports = reportService.getAllReports();
            }
            
            List<String> pageNames = reportService.getDistinctPageNames();
            
            model.addAttribute("reports", reports);
            model.addAttribute("pageNames", pageNames);
            
            return "reports";
            
        } catch (Exception e) {
            model.addAttribute("error", "Error loading reports: " + e.getMessage());
            return "error";
        }
    }

    @GetMapping("/reports/{reportId}")
    public String reportDetails(@PathVariable Long reportId, Model model) {
        try {
            PageAnomalyReport report = reportService.getReport(reportId)
                    .orElseThrow(() -> new RuntimeException("Report not found: " + reportId));
            
            List<Map<String, Object>> reportDetails = reportService.getReportDetails(reportId);
            
            model.addAttribute("report", report);
            model.addAttribute("reportDetails", reportDetails);
            
            return "report-details";
            
        } catch (Exception e) {
            model.addAttribute("error", "Error loading report details: " + e.getMessage());
            return "error";
        }
    }

    @GetMapping("/pages")
    public String pagesManagement(Model model) {
        try {
            List<PageConfiguration> pages = pageValidationService.getAllPages();
            model.addAttribute("pages", pages);
            
            return "pages";
            
        } catch (Exception e) {
            model.addAttribute("error", "Error loading pages: " + e.getMessage());
            return "error";
        }
    }

    @GetMapping("/health")
    public String healthCheck(Model model) {
        try {
            boolean coreServiceHealthy = argusCoreClient.isHealthy();
            
            model.addAttribute("coreServiceHealthy", coreServiceHealthy);
            model.addAttribute("schedulerServiceHealthy", true); // If we're here, scheduler is up
            
            return "health";
            
        } catch (Exception e) {
            model.addAttribute("error", "Error checking health: " + e.getMessage());
            return "error";
        }
    }
}