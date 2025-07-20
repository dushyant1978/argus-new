package com.argus.core.model;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

public class AnomalyRequest {
    
    @NotBlank(message = "Banner URL is required")
    private String bannerURL;
    
    @NotNull(message = "Curated ID is required")
    private Long curatedId;

    public AnomalyRequest() {}

    public AnomalyRequest(String bannerURL, Long curatedId) {
        this.bannerURL = bannerURL;
        this.curatedId = curatedId;
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

    @Override
    public String toString() {
        return "AnomalyRequest{" +
                "bannerURL='" + bannerURL + '\'' +
                ", curatedId=" + curatedId +
                '}';
    }
}