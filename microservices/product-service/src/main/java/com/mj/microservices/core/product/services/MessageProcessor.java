package com.mj.microservices.core.product.services;

import com.mj.api.core.product.Product;
import com.mj.api.core.product.ProductService;
import com.mj.api.event.Event;
import com.mj.util.exceptions.EventProcessingException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.cloud.stream.annotation.EnableBinding;
import org.springframework.cloud.stream.annotation.StreamListener;
import org.springframework.cloud.stream.messaging.Sink;

@EnableBinding(Sink.class)
@Slf4j
@RequiredArgsConstructor
public class MessageProcessor {

    private final ProductService productService;

    @StreamListener(target = Sink.INPUT)
    public void process(Event<Integer, Product> event) {
        log.info("Process message created at {}...", event.getEventCreatedAt());

        switch (event.getEventType()) {
            case CREATE -> {
                Product product = event.getData();
                log.info("Create product with Id: {}", product.getProductId());
                productService.createProduct(product);
            }
            case DELETE -> {
                int productId = event.getKey();
                log.info("Delete recommendations with ProductId: {}", productId);
                productService.deleteProduct(productId);
            }
            default -> {
                String errorMessage = "Incorrect event type: " + event.getEventType()
                    + ", expected a CREATE or DELETE event";
                log.warn(errorMessage);
                throw new EventProcessingException(errorMessage);
            }
        }

        log.info("Message processing done!");
    }
}
