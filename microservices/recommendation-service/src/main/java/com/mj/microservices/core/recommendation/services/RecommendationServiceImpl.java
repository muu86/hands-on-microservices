package com.mj.microservices.core.recommendation.services;

import com.mj.api.core.recommendation.Recommendation;
import com.mj.api.core.recommendation.RecommendationService;
import com.mj.microservices.core.recommendation.persistence.RecommendationEntity;
import com.mj.microservices.core.recommendation.persistence.RecommendationRepository;
import com.mj.util.exceptions.InvalidInputException;
import com.mj.util.http.ServiceUtil;
import java.util.ArrayList;
import java.util.List;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.web.bind.annotation.RestController;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

@RestController
@Slf4j
public class RecommendationServiceImpl implements RecommendationService {

    private final RecommendationRepository repository;
    private final RecommendationMapper mapper;
    private final ServiceUtil serviceUtil;

    @Autowired
    public RecommendationServiceImpl(
        RecommendationRepository repository,
        RecommendationMapper mapper,
        ServiceUtil serviceUtil) {
        this.repository = repository;
        this.mapper = mapper;
        this.serviceUtil = serviceUtil;
    }

//    @Override
//    public List<Recommendation> getRecommendations(int productId) {
//
//        if (productId < 1) throw new InvalidInputException("Invalid productId: " + productId);
//
//        List<RecommendationEntity> entityList = repository.findByProductId(productId);
//        List<Recommendation> list = mapper.entityListToApiList(entityList);
//        list.forEach(r -> r.setServiceAddress(serviceUtil.getServiceAddress()));
//
//        log.debug("/recommendation response size: {}", list.size());
//
//        return list;
//    }

    @Override
    public Flux<Recommendation> getRecommendations(int productId) {
        if (productId < 1) {
            throw new InvalidInputException("Invalid productId: " + productId);
        }

        return repository.findByProductId(productId)
            .log()
            .map(e -> mapper.entityToApi(e))
            .map(e -> {
                e.setServiceAddress(serviceUtil.getServiceAddress());
                return e;
            });
    }

//    @Override
//    public Recommendation createRecommendation(Recommendation body) {
//        try {
//            RecommendationEntity entity = mapper.apiToEntity(body);
//            RecommendationEntity saved = repository.save(entity);
//
//            log.debug("createRecommendation: created a recommendation entity: {}/{}",
//                body.getProductId(), body.getRecommendationId());
//            return mapper.entityToApi(saved);
//        } catch (DuplicateKeyException e) {
//            throw new InvalidInputException("Duplicate Key, ProductId: + " + body.getProductId() + "Recommendation Id: ," + body.getRecommendationId());
//        }
//    }


    @Override
    public Recommendation createRecommendation(Recommendation body) {
        if (body.getProductId() < 1) {
            throw new InvalidInputException("Invalid productId: " + body.getProductId());
        }

        RecommendationEntity entity = mapper.apiToEntity(body);
        Mono<Recommendation> newEntity = repository.save(entity)
            .log()
            .onErrorMap(
                DuplicateKeyException.class,
                ex -> new InvalidInputException(
                    "Duplicate key, Product Id: " + body.getProductId() + ", Recommendation Id: "
                        + body.getRecommendationId()))
            .map(e -> mapper.entityToApi(e));

        return newEntity.block();
    }

//    @Override
//    public void deleteRecommendations(int productId) {
//        log.debug("deleteRecommendations: tries to delete recommendations for the product with productId: {}", productId);
//        repository.deleteAll(repository.findByProductId(productId));
//    }


    @Override
    public void deleteRecommendations(int productId) {
        if (productId < 1) {
            throw new InvalidInputException("Invalid productId: " + productId);
        }

        log.debug("deleteRecommendations: tries to delete recommendations for the product with productId: {}", productId);
        repository.deleteAll(repository.findByProductId(productId)).block();
    }
}
