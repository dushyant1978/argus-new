package com.argus.core.model;

import java.util.List;

public class BannerInfo {
    
    private List<String> brands;
    private DiscountRange discountRange;
    
    public BannerInfo() {}
    
    public BannerInfo(List<String> brands, DiscountRange discountRange) {
        this.brands = brands;
        this.discountRange = discountRange;
    }

    public List<String> getBrands() {
        return brands;
    }

    public void setBrands(List<String> brands) {
        this.brands = brands;
    }

    public DiscountRange getDiscountRange() {
        return discountRange;
    }

    public void setDiscountRange(DiscountRange discountRange) {
        this.discountRange = discountRange;
    }

    public static class DiscountRange {
        private Double lower;
        private Double upper;

        public DiscountRange() {}

        public DiscountRange(Double lower, Double upper) {
            this.lower = lower;
            this.upper = upper;
        }

        public Double getLower() {
            return lower;
        }

        public void setLower(Double lower) {
            this.lower = lower;
        }

        public Double getUpper() {
            return upper;
        }

        public void setUpper(Double upper) {
            this.upper = upper;
        }

        @Override
        public String toString() {
            return "DiscountRange{" +
                    "lower=" + lower +
                    ", upper=" + upper +
                    '}';
        }
    }

    @Override
    public String toString() {
        return "BannerInfo{" +
                "brands=" + brands +
                ", discountRange=" + discountRange +
                '}';
    }
}