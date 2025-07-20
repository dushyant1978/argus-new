package com.argus.scheduler.service;

import com.argus.scheduler.model.PageAnomalyReport;
import com.argus.scheduler.repository.PageAnomalyReportRepository;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.Optional;

@Service
public class ReportService {

    private static final Logger logger = LoggerFactory.getLogger(ReportService.class);
    
    @Autowired
    private PageAnomalyReportRepository reportRepository;
    
    @Autowired
    private ObjectMapper objectMapper;

    public PageAnomalyReport saveReport(PageAnomalyReport report) {
        logger.info("Saving anomaly report for page: {}", report.getPageName());
        return reportRepository.save(report);
    }

    public List<PageAnomalyReport> getAllReports() {
        return reportRepository.findAllOrderByScanTimeDesc();
    }

    public List<PageAnomalyReport> getReportsForPage(String pageName) {
        return reportRepository.findByPageNameOrderByScanTimeDesc(pageName);
    }

    public List<PageAnomalyReport> getRecentReports(LocalDateTime since) {
        return reportRepository.findReportsAfter(since);
    }

    public Optional<PageAnomalyReport> getReport(Long id) {
        return reportRepository.findById(id);
    }

    public List<String> getDistinctPageNames() {
        return reportRepository.findDistinctPageNames();
    }

    public List<Map<String, Object>> getReportDetails(Long reportId) {
        try {
            Optional<PageAnomalyReport> reportOpt = reportRepository.findById(reportId);
            if (reportOpt.isEmpty()) {
                throw new IllegalArgumentException("Report with id " + reportId + " not found");
            }
            
            PageAnomalyReport report = reportOpt.get();
            String reportData = report.getReportData();
            
            if (reportData == null || reportData.trim().isEmpty()) {
                return List.of();
            }
            
            TypeReference<List<Map<String, Object>>> typeRef = new TypeReference<List<Map<String, Object>>>() {};
            return objectMapper.readValue(reportData, typeRef);
            
        } catch (Exception e) {
            logger.error("Error parsing report details for report {}: {}", reportId, e.getMessage());
            return List.of();
        }
    }

    public void deleteOldReports(int daysToKeep) {
        LocalDateTime cutoffDate = LocalDateTime.now().minusDays(daysToKeep);
        List<PageAnomalyReport> oldReports = reportRepository.findReportsAfter(cutoffDate);
        
        logger.info("Deleting {} old reports (older than {} days)", oldReports.size(), daysToKeep);
        reportRepository.deleteAll(oldReports);
    }

    public long getTotalReportsCount() {
        return reportRepository.count();
    }

    public long getTotalAnomaliesAcrossAllReports() {
        List<PageAnomalyReport> allReports = reportRepository.findAll();
        return allReports.stream()
                .mapToLong(PageAnomalyReport::getTotalAnomalies)
                .sum();
    }
}