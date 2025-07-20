package com.argus.core.model;

import com.fasterxml.jackson.annotation.JsonProperty;

public class Product {
    
    private String code;
    
    @JsonProperty("fnlColorVariantData")
    private FnlColorVariantData fnlColorVariantData;
    
    private Double discountPercent;
    
    public Product() {}

    public String getCode() {
        return code;
    }

    public void setCode(String code) {
        this.code = code;
    }

    public FnlColorVariantData getFnlColorVariantData() {
        return fnlColorVariantData;
    }

    public void setFnlColorVariantData(FnlColorVariantData fnlColorVariantData) {
        this.fnlColorVariantData = fnlColorVariantData;
    }

    public Double getDiscountPercent() {
        return discountPercent;
    }

    public void setDiscountPercent(Double discountPercent) {
        this.discountPercent = discountPercent;
    }

    public static class FnlColorVariantData {
        private String brandName;

        public FnlColorVariantData() {}

        public String getBrandName() {
            return brandName;
        }

        public void setBrandName(String brandName) {
            this.brandName = brandName;
        }

        @Override
        public String toString() {
            return "FnlColorVariantData{" +
                    "brandName='" + brandName + '\'' +
                    '}';
        }
    }

    @Override
    public String toString() {
        return "Product{" +
                "code='" + code + '\'' +
                ", fnlColorVariantData=" + fnlColorVariantData +
                ", discountPercent=" + discountPercent +
                '}';
    }
}