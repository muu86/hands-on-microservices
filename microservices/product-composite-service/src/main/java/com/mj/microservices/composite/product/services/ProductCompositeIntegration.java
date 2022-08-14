package com.mj.microservices.composite.product.services;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.mj.api.core.product.Product;
import com.mj.api.core.product.ProductService;
import com.mj.api.core.recommendation.Recommendation;
import com.mj.api.core.recommendation.RecommendationService;
import com.mj.api.core.review.Review;
import com.mj.api.core.review.ReviewService;
import com.mj.api.event.Event;
import com.mj.api.event.Event.Type;
import com.mj.util.exceptions.InvalidInputException;
import com.mj.util.exceptions.NotFoundException;
import com.mj.util.http.HttpErrorInfo;
import java.io.IOException;
import java.util.List;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.actuate.health.Health;
import org.springframework.cloud.stream.annotation.EnableBinding;
import org.springframework.cloud.stream.annotation.Output;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.http.HttpMethod;
import org.springframework.messaging.MessageChannel;
import org.springframework.messaging.support.MessageBuilder;
import org.springframework.stereotype.Component;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.reactive.function.client.WebClient;
import org.springframework.web.reactive.function.client.WebClientResponseException;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

@Component
@EnableBinding(ProductCompositeIntegration.MessageSources.class)
public class ProductCompositeIntegration implements ProductService, RecommendationService,
    ReviewService {

    private static final Logger log = LoggerFactory.getLogger(ProductCompositeIntegration.class);

//    private final RestTemplate restTemplate;
    private final WebClient webClient;
    private final ObjectMapper mapper;
    private final String productServiceUrl;
    private final String recommendationServiceUrl;
    private final String reviewServiceUrl;
    private MessageSources messageSources;

    public interface MessageSources {

        String OUTPUT_PRODUCTS = "output-products";
        String OUTPUT_RECOMMENDATIONS = "output-recommendations";
        String OUTPUT_REVIEWS = "output-reviews";

        @Output(OUTPUT_PRODUCTS)
        MessageChannel outputProducts();

        @Output(OUTPUT_RECOMMENDATIONS)
        MessageChannel outputRecommendations();

        @Output(OUTPUT_REVIEWS)
        MessageChannel outputReviews();
    }

    public ProductCompositeIntegration(
//        RestTemplate restTemplate,
        WebClient.Builder webClient, ObjectMapper mapper,
        MessageSources messageSources,
        @Value("${app.product-service.host}") String productServiceHost,
        @Value("${app.product-service.port}") String productServicePort,
        @Value("${app.recommendation-service.host}") String recommedationServiceHost,
        @Value("${app.recommendation-service.port}") String recommedationServicePort,
        @Value("${app.review-service.host}") String reviewServiceHost,
        @Value("${app.review-service.port}") String reviewServicePort
        ) {

        this.webClient = webClient.build();
//        this.restTemplate = restTemplate;
        this.mapper = mapper;
        this.messageSources = messageSources;

        this.productServiceUrl = "http://" + productServiceHost + ":" + productServicePort;
        this.recommendationServiceUrl = "http://" + recommedationServiceHost + ":" + recommedationServicePort;
        this.reviewServiceUrl = "http://" + reviewServiceHost + ":" + reviewServicePort;
    }

    @Override
    public Product createProduct(Product body) {
//        try {
//            String url = productServiceUrl;
//            log.debug("Will post a new product to Url: {}", url);
//
//            Product product = restTemplate.postForObject(url, body, Product.class);
//            log.debug("Created a product with id: {}", product.getProductId());
//
//            return product;
//
//        } catch (HttpClientErrorException e) {
//            throw handleHttpClientException(e);
//        }

        messageSources.outputProducts()
            .send(MessageBuilder.withPayload(new Event(Type.CREATE, body.getProductId(), body)).build());
        return body;
    }

    @Override
    public Mono<Product> getProduct(int productId) {
        /*try {
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
        }*/

        String url = productServiceUrl + "/product/" + productId;
        log.debug("Will call the getProduct API call on URL: {}", url);

        return webClient.get()
            .uri(url)
            .retrieve()
            .bodyToMono(Product.class)
            .log()
            .onErrorMap(WebClientResponseException.class, e -> handleException(e));
    }

    @Override
    public void deleteProduct(int productId) {
//        try {
//            String url = productServiceUrl + "/" + productId;
//            log.debug("Will call the deleteProduct API on URL: {}", url);
//
//            restTemplate.delete(url);
//
//        } catch (HttpClientErrorException ex) {
//            throw handleHttpClientException(ex);
//        }

        messageSources.outputProducts()
            .send(MessageBuilder.withPayload(new Event(Type.DELETE, productId, null)).build());
    }

    @Override
    public Recommendation createRecommendation(Recommendation body) {
//        try {
//            String url = recommendationServiceUrl;
//            log.debug("Will post a new recommendation to URL: {}", url);
//
//            Recommendation recommendation = restTemplate.postForObject(url, body, Recommendation.class);
//            log.debug("Created a recommendation with id: {}", recommendation.getProductId());
//
//            return recommendation;
//
//        } catch (HttpClientErrorException ex) {
//            throw handleHttpClientException(ex);
//        }

        messageSources.outputRecommendations()
            .send(MessageBuilder.withPayload(new Event(Type.CREATE, body.getProductId(), body)).build());
        return body;
    }

//    @Override
//    public List<Recommendation> getRecommendations(int productId) {
//        String url = recommendationServiceUrl + productId;
//        List<Recommendation> recommendations = restTemplate.exchange(url, HttpMethod.GET, null,
//            new ParameterizedTypeReference<List<Recommendation>>() {
//            }).getBody();
//        return recommendations;
//    }

    @Override
    public Flux<Recommendation> getRecommendations(int productId) {
        String url = recommendationServiceUrl + "/recommendation?productId=" + productId;
        log.debug("Will call the getRecommendations API on URL: {}", url);

        return webClient.get()
            .uri(url)
            .retrieve()
            .bodyToFlux(Recommendation.class)
            .log()
            .onErrorResume(e -> Flux.empty());
    }

    @Override
    public void deleteRecommendations(int productId) {
        /*try {
            String url = recommendationServiceUrl + "?productId=" + productId;
            log.debug("Will call the deleteRecommendations API on URL: {}", url);

            restTemplate.delete(url);

        } catch (HttpClientErrorException ex) {
            throw handleHttpClientException(ex);
        }*/
        messageSources.outputRecommendations()
            .send(MessageBuilder.withPayload(new Event(Type.DELETE, productId, null)).build());
    }

    @Override
    public Review createReview(Review body) {

        /*try {
            String url = reviewServiceUrl;
            log.debug("Will post a new review to URL: {}", url);

            Review review = restTemplate.postForObject(url, body, Review.class);
            log.debug("Created a review with id: {}", review.getProductId());

            return review;

        } catch (HttpClientErrorException ex) {
            throw handleHttpClientException(ex);
        }*/

        messageSources.outputReviews()
            .send(MessageBuilder.withPayload(
                new Event(Type.CREATE, body.getProductId(), body)).build());

        return body;
    }

    /*@Override
    public List<Review> getReviews(int productId) {
        String url = reviewServiceUrl + productId;
        List<Review> reviews = restTemplate.exchange(url, HttpMethod.GET, null,
            new ParameterizedTypeReference<List<Review>>() {
            }).getBody();
        return reviews;
    }*/

    @Override
    public Flux<Review> getReviews(int productId) {
        String url = reviewServiceUrl + "/review?productId=" + productId;
        log.debug("Will call the getReviews API on URL: {}", url);

        return webClient.get()
            .uri(url)
            .retrieve()
            .bodyToFlux(Review.class)
            .log()
            .onErrorResume(error -> Flux.empty());
    }

    @Override
    public void deleteReviews(int productId) {
        /*try {
            String url = reviewServiceUrl + "?productId=" + productId;
            log.debug("Will call the deleteReviews API on URL: {}", url);

            restTemplate.delete(url);

        } catch (HttpClientErrorException ex) {
            throw handleHttpClientException(ex);
        }*/

        messageSources.outputReviews()
            .send(MessageBuilder.withPayload(new Event(Type.DELETE, productId, null)).build());
    }

    private Mono<Health> getHealth(String url) {
        url += "/actuator/health";
        log.debug("Will call the Health API on URL: {}", url);
        return webClient.get()
            .uri(url)
            .retrieve()
            .bodyToMono(String.class)
            .map(s -> new Health.Builder().up().build())
            .onErrorResume(e -> Mono.just(new Health.Builder().down(e).build()))
            .log();
    }

    public Mono<Health> getProductHealth() {
        return getHealth(productServiceUrl);
    }

    public Mono<Health> getRecommendationHealth() {
        return getHealth(recommendationServiceUrl);
    }

    public Mono<Health> getReviewHealth() {
        return getHealth(reviewServiceUrl);
    }

    private Throwable handleException(Throwable e) {

        if (!(e instanceof WebClientResponseException)) {
            log.warn("Got a unexpected error: {}, will rethrow it", e.toString());
            return e;
        }

        WebClientResponseException wcre = (WebClientResponseException) e;

        switch (wcre.getStatusCode()) {
            case NOT_FOUND -> {
                return new NotFoundException(getErrorMessage(wcre));
            }
            case UNPROCESSABLE_ENTITY -> {
                return new InvalidInputException(getErrorMessage(wcre));
            }
            default -> {
                log.warn("Got a unexpected Http error: {} will rethrow it", wcre.getStatusCode());
                log.warn("Error body: {}", wcre.getResponseBodyAsString());
                return e;
            }
        }
    }

    private String getErrorMessage(WebClientResponseException e) {
        try {
            return mapper.readValue(e.getResponseBodyAsString(), HttpErrorInfo.class).getMessage();
        } catch (IOException ioex) {
            return e.getMessage();
        }
    }
}
