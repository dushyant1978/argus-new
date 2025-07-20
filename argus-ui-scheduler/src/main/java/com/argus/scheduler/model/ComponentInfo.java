package com.argus.scheduler.model;

public class ComponentInfo {
    
    private String bannerURL;
    private Long curatedId;
    private String componentType;
    
    public ComponentInfo() {}
    
    public ComponentInfo(String bannerURL, Long curatedId, String componentType) {
        this.bannerURL = bannerURL;
        this.curatedId = curatedId;
        this.componentType = componentType;
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

    public String getComponentType() {
        return componentType;
    }

    public void setComponentType(String componentType) {
        this.componentType = componentType;
    }

    @Override
    public String toString() {
        return "ComponentInfo{" +
                "bannerURL='" + bannerURL + '\'' +
                ", curatedId=" + curatedId +
                ", componentType='" + componentType + '\'' +
                '}';
    }
}