package com.argus.scheduler.repository;

import com.argus.scheduler.model.PageAnomalyReport;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;

@Repository
public interface PageAnomalyReportRepository extends JpaRepository<PageAnomalyReport, Long> {
    
    List<PageAnomalyReport> findByPageNameOrderByScanTimeDesc(String pageName);
    
    @Query("SELECT r FROM PageAnomalyReport r WHERE r.scanTime >= ?1 ORDER BY r.scanTime DESC")
    List<PageAnomalyReport> findReportsAfter(LocalDateTime scanTime);
    
    @Query("SELECT r FROM PageAnomalyReport r ORDER BY r.scanTime DESC")
    List<PageAnomalyReport> findAllOrderByScanTimeDesc();
    
    @Query("SELECT DISTINCT r.pageName FROM PageAnomalyReport r ORDER BY r.pageName")
    List<String> findDistinctPageNames();
}