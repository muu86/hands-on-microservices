package com.mj.api.core.product;

import lombok.Getter;
import lombok.Setter;

@Setter
@Getter
public class Product {
    private int productId;
    private String name;
    private int weight;
    private String serviceAddress;

    public Product() {
        productId = 0;
        name = null;
        weight = 0;
        serviceAddress = null;
    }

    public Product(int productId, String name, int weight, String serviceAddress) {
        this.productId = productId;
        this.name = name;
        this.weight = weight;
        this.serviceAddress = serviceAddress;
    }

}
