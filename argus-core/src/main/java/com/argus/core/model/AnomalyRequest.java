package com.argus.core.model;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

public class AnomalyRequest {
    
    @NotBlank(message = "Banner URL is required")
    private String bannerURL;
    
    @NotBlank(message = "Curated ID is required")
    private String curatedId;

    public AnomalyRequest() {}

    public AnomalyRequest(String bannerURL, String curatedId) {
        this.bannerURL = bannerURL;
        this.curatedId = curatedId;
    }

    public String getBannerURL() {
        return bannerURL;
    }

    public void setBannerURL(String bannerURL) {
        this.bannerURL = bannerURL;
    }

    public String getCuratedId() {
        return curatedId;
    }

    public void setCuratedId(String curatedId) {
        this.curatedId = curatedId;
    }

    @Override
    public String toString() {
        return "AnomalyRequest{" +
                "bannerURL='" + bannerURL + '\'' +
                ", curatedId='" + curatedId + '\'' +
                '}';
    }
}