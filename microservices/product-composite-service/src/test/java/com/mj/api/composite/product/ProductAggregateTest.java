package com.mj.api.composite.product;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

class ProductAggregateTest {

    private static ObjectMapper mapper = new ObjectMapper();

    @Test
    void mappingProductAggregate() throws JsonProcessingException {
        String data = "{\"productId\":1,\"name\":\"name\",\"weight\":1,\"recommendations\":[{\"recommendationId\":1,\"author"
            + "\":\"a\",\"rate\":1,\"content\":\"c\"}],\"reviews\":[{\"reviewId\":1,\"author\":\"a\",\"subject\":\"s\",\"content\":\"c\"}],"
            + "\"serviceAddresses\":null}";
        ProductAggregate result = mapper.readValue(data, ProductAggregate.class);
        System.out.println(result);
    }
}