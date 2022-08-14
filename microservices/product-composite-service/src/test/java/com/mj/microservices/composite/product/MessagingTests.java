package com.mj.microservices.composite.product;

import static com.mj.microservices.composite.product.IsSameEvent.sameEventExceptCreatedAt;
import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.is;
import static org.springframework.cloud.stream.test.matcher.MessageQueueMatcher.receivesPayloadThat;

import com.mj.api.composite.product.ProductAggregate;
import com.mj.api.composite.product.RecommendationSummary;
import com.mj.api.composite.product.ReviewSummary;
import com.mj.api.core.product.Product;
import com.mj.api.core.recommendation.Recommendation;
import com.mj.api.core.review.Review;
import com.mj.api.event.Event;
import com.mj.api.event.Event.Type;
import com.mj.microservices.composite.product.services.ProductCompositeIntegration;
import java.util.Collections;
import java.util.concurrent.BlockingQueue;
import org.hamcrest.MatcherAssert;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.context.SpringBootTest.WebEnvironment;
import org.springframework.cloud.stream.test.binder.MessageCollector;
import org.springframework.http.HttpStatus;
import org.springframework.messaging.Message;
import org.springframework.messaging.MessageChannel;
import org.springframework.test.web.reactive.server.WebTestClient;
import reactor.core.publisher.Mono;

@SpringBootTest(webEnvironment = WebEnvironment.RANDOM_PORT)
//@AutoConfigureWebTestClient
public class MessagingTests {

    public static final int PRODUCT_ID_OK = 1;
    public static final int PRODUCT_ID_NOT_FOUND = 2;
    public static final int PRODUCT_ID_INVALID = 3;

    @Autowired
    private WebTestClient client;

    @Autowired
    private ProductCompositeIntegration.MessageSources channels;

    @Autowired
    private MessageCollector collector;

    BlockingQueue<Message<?>> queueProducts = null;
    BlockingQueue<Message<?>> queueRecommendations = null;
    BlockingQueue<Message<?>> queueReviews = null;

    @BeforeEach
    void setup() {
        queueProducts = getQueue(channels.outputProducts());
        queueRecommendations = getQueue(channels.outputRecommendations());
        queueReviews = getQueue(channels.outputReviews());
    }

    @Test
    void createCompositeProduct1() {
        ProductAggregate composite = new ProductAggregate(1, "name", 1, null, null, null);
        postAndVerifyProduct(composite, HttpStatus.OK);

//        assertEquals(1, queueProducts.size());
        assertThat(queueProducts.size()).isEqualTo(1);

        Event<Integer, Product> expectedEvent = new Event(
            Type.CREATE, composite.getProductId(),
            new Product(
                composite.getProductId(),
                composite.getName(),
                composite.getWeight(),
                null));
        MatcherAssert.assertThat(queueProducts, is(receivesPayloadThat(
            sameEventExceptCreatedAt(expectedEvent))));

        assertThat(queueRecommendations.size()).isEqualTo(0);
        assertThat(queueReviews.size()).isEqualTo(0);
    }

    @Test
    public void createCompositeProduct2() {
        ProductAggregate composite = new ProductAggregate(1, "name", 1,
            Collections.singletonList(new RecommendationSummary(1, "a", 1, "c")),
            Collections.singletonList(new ReviewSummary(1, "a", "s", "c")),
            null);

        postAndVerifyProduct(composite, HttpStatus.OK);

        assertThat(queueProducts.size()).isEqualTo(1);
        Event<Integer, Product> expectedProductEvent = new Event(Type.CREATE, composite.getProductId(),
            new Product(composite.getProductId(), composite.getName(), composite.getWeight(),
                null));
        MatcherAssert.assertThat(
            queueProducts,
            receivesPayloadThat(sameEventExceptCreatedAt(expectedProductEvent)));

        assertThat(queueRecommendations.size()).isEqualTo(1);
        RecommendationSummary rec = composite.getRecommendations().get(0);
        Event expectedRecommendationEvent = new Event(Type.CREATE, composite.getProductId(),
            new Recommendation(composite.getProductId(), rec.getRecommendationId(), rec.getAuthor(),
                rec.getRate(), rec.getContent(), null));
        MatcherAssert.assertThat(queueRecommendations,
            receivesPayloadThat(sameEventExceptCreatedAt(expectedRecommendationEvent)));

        assertThat(queueReviews.size()).isEqualTo(1);
        ReviewSummary rev = composite.getReviews().get(0);
        Event expectedReviewEvent = new Event(Type.CREATE, composite.getProductId(),
            new Review(composite.getProductId(), rev.getReviewId(), rev.getAuthor(),
                rev.getSubject(), rev.getContent(), null));
        MatcherAssert.assertThat(queueReviews,
            receivesPayloadThat(
                sameEventExceptCreatedAt(expectedReviewEvent)));
    }

    @Test
    public void deleteCompositeProduct() {
        deleteAndVerifyProduct(1, HttpStatus.OK);

        assertThat(queueProducts.size()).isEqualTo(1);
        Event<Integer, Product> expectedProductEvent = new Event(Type.DELETE, 1, null);
        MatcherAssert.assertThat(queueProducts,
            is(receivesPayloadThat(sameEventExceptCreatedAt(expectedProductEvent))));

        assertThat(queueRecommendations.size()).isEqualTo(1);
        Event<Integer, Product> expectedRecommendationEvent = new Event(Type.DELETE, 1, null);
        MatcherAssert.assertThat(queueRecommendations,
            is(receivesPayloadThat(sameEventExceptCreatedAt(expectedRecommendationEvent))));

        assertThat(queueReviews.size()).isEqualTo(1);
        Event<Integer, Product> expectedReviewEvent = new Event(Type.DELETE, 1, null);
        MatcherAssert.assertThat(queueReviews,
            is(receivesPayloadThat(sameEventExceptCreatedAt(expectedReviewEvent))));
    }

    private void postAndVerifyProduct(ProductAggregate composite, HttpStatus status) {
        client.post()
            .uri("/product-composite")
            .body(Mono.just(composite), ProductAggregate.class)
            .exchange()
            .expectStatus().isEqualTo(status);
    }

    private void deleteAndVerifyProduct(int productId, HttpStatus status) {
        client.delete()
            .uri("/product-composite/" + productId)
            .exchange()
            .expectStatus().isEqualTo(status);
    }

    private BlockingQueue<Message<?>> getQueue(MessageChannel messageChannel) {
        return collector.forChannel(messageChannel);
    }
}
