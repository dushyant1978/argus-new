package com.argus.core.controller;

import com.argus.core.model.AnomalyRequest;
import com.argus.core.model.AnomalyResponse;
import com.argus.core.service.AnomalyDetectionService;
import jakarta.validation.Valid;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequestMapping("/api/v1/anomaly")
public class AnomalyDetectionController {

    private static final Logger logger = LoggerFactory.getLogger(AnomalyDetectionController.class);
    
    @Autowired
    private AnomalyDetectionService anomalyDetectionService;

    @PostMapping("/detect")
    public ResponseEntity<AnomalyResponse> detectAnomalies(@Valid @RequestBody AnomalyRequest request) {
        logger.info("Received anomaly detection request: {}", request);
        
        try {
            AnomalyResponse response = anomalyDetectionService.detectAnomalies(request);
            
            if ("error".equals(response.getStatus())) {
                return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(response);
            }
            
            logger.info("Successfully processed anomaly detection request. Found {} anomalies", 
                       response.getTotalAnomalies());
            
            return ResponseEntity.ok(response);
            
        } catch (Exception e) {
            logger.error("Unexpected error during anomaly detection: {}", e.getMessage(), e);
            
            AnomalyResponse errorResponse = new AnomalyResponse();
            errorResponse.setBannerURL(request.getBannerURL());
            errorResponse.setCuratedId(request.getCuratedId());
            errorResponse.setStatus("error");
            errorResponse.setMessage("Unexpected error: " + e.getMessage());
            
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(errorResponse);
        }
    }

    @GetMapping("/health")
    public ResponseEntity<Map<String, String>> health() {
        return ResponseEntity.ok(Map.of(
                "status", "UP",
                "service", "argus-core",
                "version", "1.0.0"
        ));
    }
}