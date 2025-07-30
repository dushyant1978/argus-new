package com.argus.scheduler.model;

import jakarta.persistence.*;
import java.time.LocalDateTime;
import java.util.List;

@Entity
@Table(name = "page_anomaly_reports")
public class PageAnomalyReport {
    
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    
    private String pageName;
    private String pageId;
    private int totalComponents;
    private int componentsWithAnomalies;
    private int totalAnomalies;
    
    @Column(columnDefinition = "TEXT")
    private String reportData;
    
    @Column(name = "scan_time")
    private LocalDateTime scanTime;
    
    private String status;
    
    public PageAnomalyReport() {}
    
    public PageAnomalyReport(String pageName, String pageId, int totalComponents, 
                           int componentsWithAnomalies, int totalAnomalies, 
                           String reportData, String status) {
        this.pageName = pageName;
        this.pageId = pageId;
        this.totalComponents = totalComponents;
        this.componentsWithAnomalies = componentsWithAnomalies;
        this.totalAnomalies = totalAnomalies;
        this.reportData = reportData;
        this.status = status;
        this.scanTime = LocalDateTime.now();
    }
    
    @PrePersist
    protected void onCreate() {
        scanTime = LocalDateTime.now();
    }

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public String getPageName() {
        return pageName;
    }

    public void setPageName(String pageName) {
        this.pageName = pageName;
    }

    public String getPageId() {
        return pageId;
    }

    public void setPageId(String pageId) {
        this.pageId = pageId;
    }

    public int getTotalComponents() {
        return totalComponents;
    }

    public void setTotalComponents(int totalComponents) {
        this.totalComponents = totalComponents;
    }

    public int getComponentsWithAnomalies() {
        return componentsWithAnomalies;
    }

    public void setComponentsWithAnomalies(int componentsWithAnomalies) {
        this.componentsWithAnomalies = componentsWithAnomalies;
    }

    public int getTotalAnomalies() {
        return totalAnomalies;
    }

    public void setTotalAnomalies(int totalAnomalies) {
        this.totalAnomalies = totalAnomalies;
    }

    public String getReportData() {
        return reportData;
    }

    public void setReportData(String reportData) {
        this.reportData = reportData;
    }

    public LocalDateTime getScanTime() {
        return scanTime;
    }

    public void setScanTime(LocalDateTime scanTime) {
        this.scanTime = scanTime;
    }

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }

    @Override
    public String toString() {
        return "PageAnomalyReport{" +
                "id=" + id +
                ", pageName='" + pageName + '\'' +
                ", pageId='" + pageId + '\'' +
                ", totalComponents=" + totalComponents +
                ", componentsWithAnomalies=" + componentsWithAnomalies +
                ", totalAnomalies=" + totalAnomalies +
                ", scanTime=" + scanTime +
                ", status='" + status + '\'' +
                '}';
    }
}