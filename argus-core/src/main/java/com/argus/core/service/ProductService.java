package com.argus.core.service;

import com.argus.core.client.AjioProductClient;
import com.argus.core.model.Product;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class ProductService {

    private static final Logger logger = LoggerFactory.getLogger(ProductService.class);
    
    @Autowired
    private AjioProductClient ajioProductClient;

    public List<Product> getProducts(String curatedId) {
        logger.info("Fetching products for curatedId: {}", curatedId);
        
        List<Product> products = ajioProductClient.getProducts(curatedId);
        
        logger.info("Retrieved {} products for curatedId: {}", products.size(), curatedId);
        
        return products;
    }
}