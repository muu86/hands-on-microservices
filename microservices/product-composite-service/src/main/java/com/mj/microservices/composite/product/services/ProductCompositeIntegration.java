package com.mj.microservices.composite.product.services;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.mj.api.core.product.Product;
import com.mj.api.core.product.ProductService;
import com.mj.api.core.recommendation.Recommendation;
import com.mj.api.core.recommendation.RecommendationService;
import com.mj.api.core.review.Review;
import com.mj.api.core.review.ReviewService;
import com.mj.util.exceptions.InvalidInputException;
import com.mj.util.exceptions.NotFoundException;
import com.mj.util.http.HttpErrorInfo;
import java.io.IOException;
import java.util.List;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.http.HttpMethod;
import org.springframework.stereotype.Component;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.RestTemplate;

@Component
public class ProductCompositeIntegration implements ProductService, RecommendationService,
    ReviewService {

    private static final Logger log = LoggerFactory.getLogger(ProductCompositeIntegration.class);

    private final RestTemplate restTemplate;
    private final ObjectMapper mapper;
    private final String productServiceUrl;
    private final String recommedationServiceUrl;
    private final String reviewServiceUrl;

    public ProductCompositeIntegration(
        RestTemplate restTemplate,
        ObjectMapper mapper,
        @Value("${app.product-service.host}") String productServiceHost,
        @Value("${app.product-service.port}") String productServicePort,
        @Value("${app.recommendation-service.host}") String recommedationServiceHost,
        @Value("${app.recommendation-service.port}") String recommedationServicePort,
        @Value("${app.review-service.host}") String reviewServiceHost,
        @Value("${app.review-service.port}") String reviewServicePort) {

        this.restTemplate = restTemplate;
        this.mapper = mapper;
        this.productServiceUrl = "http://" + productServiceHost + ":" + productServicePort + "/product/";
        this.recommedationServiceUrl = "http://" + recommedationServiceHost + ":" + recommedationServicePort + "/recommendation?productId=";
        this.reviewServiceUrl = "http://" + reviewServiceHost + ":" + reviewServicePort + "/review?productId=";
    }

    @Override
    public Product getProduct(int productId) {
        try {
            String url = productServiceUrl + productId;
            log.debug("Will call getProduct API on URL: {}", url);

            Product product = restTemplate.getForObject(url, Product.class);
            log.debug("Found a product with id: {}", product.getProductId());

            return product;

        } catch (HttpClientErrorException ex) {

            switch (ex.getStatusCode()) {

                case NOT_FOUND:
                    throw new NotFoundException(getErrorMessage(ex));

                case UNPROCESSABLE_ENTITY :
                    throw new InvalidInputException(getErrorMessage(ex));

                default:
                    log.warn("Got a unexpected HTTP error: {}, will rethrow it", ex.getStatusCode());
                    log.warn("Error body: {}", ex.getResponseBodyAsString());
                    throw ex;
            }
        }
    }

    private String getErrorMessage(HttpClientErrorException ex) {
        try {
            return mapper.readValue(ex.getResponseBodyAsString(), HttpErrorInfo.class).getMessage();
        } catch (IOException ioex) {
            return ex.getMessage();
        }
    }

    @Override
    public List<Recommendation> getRecommendations(int productId) {
        String url = recommedationServiceUrl + productId;
        List<Recommendation> recommendations = restTemplate.exchange(url, HttpMethod.GET, null,
            new ParameterizedTypeReference<List<Recommendation>>() {
            }).getBody();
        return recommendations;
    }

    @Override
    public List<Review> getReviews(int productId) {
        String url = reviewServiceUrl + productId;
        List<Review> reviews = restTemplate.exchange(url, HttpMethod.GET, null,
            new ParameterizedTypeReference<List<Review>>() {
            }).getBody();
        return reviews;
    }
}
