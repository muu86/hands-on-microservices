package com.mj.microservices.core.product.services;

import com.mj.api.core.product.Product;
import com.mj.api.core.product.ProductService;
import com.mj.microservices.core.product.persistence.ProductEntity;
import com.mj.microservices.core.product.persistence.ProductRepository;
import com.mj.util.exceptions.InvalidInputException;
import com.mj.util.exceptions.NotFoundException;
import com.mj.util.http.ServiceUtil;
import lombok.extern.slf4j.Slf4j;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.web.bind.annotation.RestController;
import reactor.core.publisher.Mono;

@RestController
@Slf4j
public class ProductServiceImpl implements ProductService {

    private final ProductRepository repository;
    private final ProductMapper mapper;
    private final ServiceUtil serviceUtil;

    public ProductServiceImpl(
        ProductRepository repository,
        ProductMapper mapper, ServiceUtil serviceUtil) {
        this.repository = repository;
        this.mapper = mapper;
        this.serviceUtil = serviceUtil;
    }

    public Product createProduct_blocking_model(Product body) {
//        try {
//            ProductEntity entity = mapper.apiToEntity(body);
//
//            Optional<ProductEntity> byId = repository.findByProductId(body.getProductId());
//            byId.ifPresentOrElse(e -> log.info("entity exists!"), () -> log.info("no entity!!"));
//
//            ProductEntity newEntity = repository.save(entity);
//
//            log.info("createProduct: entity created for productId: {}", body.getProductId());
//
//            return mapper.entityToApi(newEntity);
//        } catch (DuplicateKeyException e) {
//            log.info("DuplicateKeyException occurred!", e);
//            throw new InvalidInputException("Duplicate Key, ProductId: " + body.getProductId());
//        }
        return null;
    }

    @Override
    public Product createProduct(Product body) {
        if (body.getProductId() < 1) {
            throw new InvalidInputException("Invalid productId: " + body.getProductId());
        }

        ProductEntity entity = mapper.apiToEntity(body);
        Mono<Product> newEntity = repository.save(entity)
            .log()
            .onErrorMap(DuplicateKeyException.class,
                ex -> new InvalidInputException(
                    "Duplicate key, Product Id: " + body.getProductId()))
            .map(e -> mapper.entityToApi(e));

        return newEntity.block();
    }

//    @Override
//    public Product getProduct_blocking_model(int productId) {
//        if (productId < 1) throw new InvalidInputException("Invalid productId: " + productId);
////        if (productId == 13) throw new NotFoundException("No product found for productId: " + productId);
//
//        ProductEntity entity = repository.findByProductId(productId).orElseThrow(() -> {
//            return new NotFoundException("No product found for productId: " + productId);
//        });
//
//        Product product = mapper.entityToApi(entity);
//        product.setServiceAddress(serviceUtil.getServiceAddress());
//
//        return product;
//    }


    @Override
    public Mono<Product> getProduct(int productId) {
        if (productId < 1) {
            throw new InvalidInputException("Invalid productId: " + productId);
        }

        return repository.findByProductId(productId)
            .switchIfEmpty(Mono.error(new NotFoundException("No product found for productId: " + productId)))
            .log()
            .map(e -> mapper.entityToApi(e))
            .map(e -> {
                e.setServiceAddress(serviceUtil.getServiceAddress());
                return e;
            });
    }

//    @Override
//    public void deleteProduct_blocking_model(int productId) {
//        log.info("delete Product: tries to delete an entity with productId: {}", productId);
//        repository.findByProductId(productId).ifPresent(repository::delete);
//    }


    @Override
    public void deleteProduct(int productId) {
        if (productId < 1) {
            throw new InvalidInputException("Invalid productId: " + productId);
        }

        log.debug("deleteProduct: tries to delete an entity with productId: {}", productId);
        repository.findByProductId(productId)
            .log()
            .map(e -> repository.delete(e))
            .flatMap(e -> e)
            .block();
    }
}
