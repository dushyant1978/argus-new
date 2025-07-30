package com.argus.scheduler.service;

import com.argus.scheduler.model.PageConfiguration;
import com.argus.scheduler.repository.PageConfigurationRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Optional;

@Service
public class PageValidationService {

    private static final Logger logger = LoggerFactory.getLogger(PageValidationService.class);
    
    @Autowired
    private PageConfigurationRepository pageConfigRepository;

    public List<PageConfiguration> getAllPages() {
        return pageConfigRepository.findAll();
    }

    public List<PageConfiguration> getActivePages() {
        return pageConfigRepository.findByActiveTrue();
    }

    public PageConfiguration savePage(PageConfiguration page) {
        logger.info("Saving page configuration: {}", page.getPageName());
        return pageConfigRepository.save(page);
    }

    public PageConfiguration createPage(String pageName, String pageId) {
        if (pageConfigRepository.existsByPageName(pageName)) {
            throw new IllegalArgumentException("Page with name '" + pageName + "' already exists");
        }
        
        PageConfiguration page = new PageConfiguration(pageName, pageId);
        return pageConfigRepository.save(page);
    }

    public PageConfiguration updatePage(Long id, String pageName, String pageId, Boolean active) {
        Optional<PageConfiguration> existingPage = pageConfigRepository.findById(id);
        if (existingPage.isEmpty()) {
            throw new IllegalArgumentException("Page with id " + id + " not found");
        }
        
        PageConfiguration page = existingPage.get();
        page.setPageName(pageName);
        page.setPageId(pageId);
        page.setActive(active);
        
        return pageConfigRepository.save(page);
    }

    public void deletePage(Long id) {
        if (!pageConfigRepository.existsById(id)) {
            throw new IllegalArgumentException("Page with id " + id + " not found");
        }
        
        logger.info("Deleting page configuration with id: {}", id);
        pageConfigRepository.deleteById(id);
    }

    public Optional<PageConfiguration> getPage(Long id) {
        return pageConfigRepository.findById(id);
    }

    public void togglePageStatus(Long id) {
        Optional<PageConfiguration> pageOpt = pageConfigRepository.findById(id);
        if (pageOpt.isEmpty()) {
            throw new IllegalArgumentException("Page with id " + id + " not found");
        }
        
        PageConfiguration page = pageOpt.get();
        page.setActive(!page.getActive());
        
        logger.info("Toggling page {} status to: {}", page.getPageName(), page.getActive());
        pageConfigRepository.save(page);
    }
}