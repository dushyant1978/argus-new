package com.argus.scheduler.repository;

import com.argus.scheduler.model.PageConfiguration;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface PageConfigurationRepository extends JpaRepository<PageConfiguration, Long> {
    
    List<PageConfiguration> findByActiveTrue();
    
    boolean existsByPageName(String pageName);
}