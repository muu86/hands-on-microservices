package com.mj.microservices.core.product.services;

import com.mj.api.core.product.Product;
import com.mj.api.core.product.ProductService;
import com.mj.util.exceptions.InvalidInputException;
import com.mj.util.exceptions.NotFoundException;
import com.mj.util.http.ServiceUtil;
import org.springframework.web.bind.annotation.RestController;

@RestController
public class ProductServiceImpl implements ProductService {

    private final ServiceUtil serviceUtil;

    public ProductServiceImpl(ServiceUtil serviceUtil) {
        this.serviceUtil = serviceUtil;
    }

    @Override
    public Product getProduct(int productId) {
        if (productId < 1) throw new InvalidInputException("Invalid productId: " + productId);
        if (productId == 13) throw new NotFoundException("No product found for productId: " + productId);
        return new Product(productId, "name-" + productId, 123, serviceUtil.getServiceAddress());
    }
}
