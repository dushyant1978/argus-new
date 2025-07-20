package com.argus.core.model;

import java.util.List;

public class AnomalyResponse {
    
    private String bannerURL;
    private Long curatedId;
    private BannerInfo bannerInfo;
    private List<ProductAnomaly> anomalies;
    private int totalAnomalies;
    private String status;
    private String message;
    
    public AnomalyResponse() {}
    
    public AnomalyResponse(String bannerURL, Long curatedId, BannerInfo bannerInfo, 
                          List<ProductAnomaly> anomalies) {
        this.bannerURL = bannerURL;
        this.curatedId = curatedId;
        this.bannerInfo = bannerInfo;
        this.anomalies = anomalies;
        this.totalAnomalies = anomalies != null ? anomalies.size() : 0;
        this.status = "success";
    }

    public String getBannerURL() {
        return bannerURL;
    }

    public void setBannerURL(String bannerURL) {
        this.bannerURL = bannerURL;
    }

    public Long getCuratedId() {
        return curatedId;
    }

    public void setCuratedId(Long curatedId) {
        this.curatedId = curatedId;
    }

    public BannerInfo getBannerInfo() {
        return bannerInfo;
    }

    public void setBannerInfo(BannerInfo bannerInfo) {
        this.bannerInfo = bannerInfo;
    }

    public List<ProductAnomaly> getAnomalies() {
        return anomalies;
    }

    public void setAnomalies(List<ProductAnomaly> anomalies) {
        this.anomalies = anomalies;
        this.totalAnomalies = anomalies != null ? anomalies.size() : 0;
    }

    public int getTotalAnomalies() {
        return totalAnomalies;
    }

    public void setTotalAnomalies(int totalAnomalies) {
        this.totalAnomalies = totalAnomalies;
    }

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }

    public String getMessage() {
        return message;
    }

    public void setMessage(String message) {
        this.message = message;
    }

    public static class ProductAnomaly {
        private String code;
        private String brandName;
        private Double discountPercent;
        private List<String> anomalyReasons;

        public ProductAnomaly() {}

        public ProductAnomaly(String code, String brandName, Double discountPercent, List<String> anomalyReasons) {
            this.code = code;
            this.brandName = brandName;
            this.discountPercent = discountPercent;
            this.anomalyReasons = anomalyReasons;
        }

        public String getCode() {
            return code;
        }

        public void setCode(String code) {
            this.code = code;
        }

        public String getBrandName() {
            return brandName;
        }

        public void setBrandName(String brandName) {
            this.brandName = brandName;
        }

        public Double getDiscountPercent() {
            return discountPercent;
        }

        public void setDiscountPercent(Double discountPercent) {
            this.discountPercent = discountPercent;
        }

        public List<String> getAnomalyReasons() {
            return anomalyReasons;
        }

        public void setAnomalyReasons(List<String> anomalyReasons) {
            this.anomalyReasons = anomalyReasons;
        }
    }
}