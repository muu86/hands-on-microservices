package com.mj.microservices.core.recommendation;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.fail;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.springframework.boot.test.context.SpringBootTest.WebEnvironment.RANDOM_PORT;
import static org.springframework.http.HttpStatus.BAD_REQUEST;
import static org.springframework.http.HttpStatus.OK;
import static org.springframework.http.HttpStatus.UNPROCESSABLE_ENTITY;
import static org.springframework.http.MediaType.APPLICATION_JSON;

import com.mj.api.core.product.Product;
import com.mj.api.core.recommendation.Recommendation;
import com.mj.api.event.Event;
import com.mj.api.event.Event.Type;
import com.mj.microservices.core.recommendation.persistence.RecommendationRepository;
import com.mj.util.exceptions.InvalidInputException;
import io.micrometer.core.annotation.TimedSet;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.cloud.stream.messaging.Sink;
import org.springframework.http.HttpStatus;
import org.springframework.integration.channel.AbstractMessageChannel;
import org.springframework.messaging.MessagingException;
import org.springframework.messaging.support.GenericMessage;
import org.springframework.test.web.reactive.server.WebTestClient;
import org.springframework.util.Assert;
import reactor.core.publisher.Mono;

@SpringBootTest(webEnvironment = RANDOM_PORT,
	properties = { "spring.data.mongodb.port=0",
		"spring.mongodb.embedded.version=3.6.9",
		"spring.data.mongodb.auto-index-creation=true"
	})
public class RecommendationServiceApplicationTests {

	@Autowired
	private WebTestClient client;

	@Autowired
	private RecommendationRepository repository;

	@Autowired
	private Sink channels;

	private AbstractMessageChannel input = null;

	@BeforeEach
	public void setUp() {
		input = (AbstractMessageChannel) channels.input();
		repository.deleteAll().block();
	}

	@Test
	public void getRecommendationsByProductId() {

		int productId = 1;

//
//		postAndVerifyRecommendation(productId, 1, OK);
//		postAndVerifyRecommendation(productId, 2, OK);
//		postAndVerifyRecommendation(productId, 3, OK);
//
//		assertEquals(3, repository.findByProductId(productId).size());

		sendCreateRecommendationEvent(productId, 1);
		sendCreateRecommendationEvent(productId, 2);
		sendCreateRecommendationEvent(productId, 3);

		assertThat(repository.findByProductId(productId).count().block()).isEqualTo(3);

		getAndVerifyRecommendationsByProductId(productId, OK)
			.jsonPath("$.length()").isEqualTo(3)
			.jsonPath("$[0].productId").isEqualTo(productId)
			.jsonPath("$[0].recommendationId").isEqualTo(1);
	}

	@Test
	public void duplicateError() {
		int productId = 1;
		int recommendationId = 1;

		sendCreateRecommendationEvent(productId, recommendationId);

		assertThat(repository.count().block()).isEqualTo(1);

		try {
			sendCreateRecommendationEvent(productId, recommendationId);
			fail("Expected a MessagingException here!");
		} catch (MessagingException me) {
			if (me.getCause() instanceof InvalidInputException) {
				InvalidInputException iie = (InvalidInputException) me.getCause();
				assertThat(iie.getMessage()).isEqualTo(
					"Duplicate key, Product Id: 1, Recommendation Id: 1");
			} else {
				fail("Expected a InvalidInputException as the root cause");
			}
		}

		assertThat(repository.count().block()).isEqualTo(1);
	}

	@Test
	public void deleteRecommendation() {
		int productId = 1;
		int recommendationId = 1;

		sendCreateRecommendationEvent(productId, recommendationId);
		assertThat(repository.findByProductId(productId).count().block()).isEqualTo(1);

		sendDeleteRecommendationEvent(productId);
		assertThat(repository.findByProductId(productId).count().block()).isEqualTo(0);

		sendDeleteRecommendationEvent(productId);
	}

//	@Test
//	public void getRecommendationsMissingParameter() {
//
//		client.get()
//			.uri("/recommendation")
//			.accept(APPLICATION_JSON)
//			.exchange()
//			.expectStatus().isEqualTo(BAD_REQUEST)
//			.expectHeader().contentType(APPLICATION_JSON)
//			.expectBody()
//			.jsonPath("$.path").isEqualTo("/recommendation")
//			.jsonPath("$.message").isEqualTo("Required int parameter 'productId' is not present");
//	}
//
//	@Test
//	public void getRecommendationsInvalidParameter() {
//
//		client.get()
//			.uri("/recommendation?productId=no-integer")
//			.accept(APPLICATION_JSON)
//			.exchange()
//			.expectStatus().isEqualTo(BAD_REQUEST)
//			.expectHeader().contentType(APPLICATION_JSON)
//			.expectBody()
//			.jsonPath("$.path").isEqualTo("/recommendation")
//			.jsonPath("$.message").isEqualTo("Type mismatch.");
//	}
//
//	@Test
//	public void getRecommendationsNotFound() {
//
//		int productIdNotFound = 113;
//
//		client.get()
//			.uri("/recommendation?productId=" + productIdNotFound)
//			.accept(APPLICATION_JSON)
//			.exchange()
//			.expectStatus().isOk()
//			.expectHeader().contentType(APPLICATION_JSON)
//			.expectBody()
//			.jsonPath("$.length()").isEqualTo(0);
//	}
//
//	@Test
//	public void getRecommendationsInvalidParameterNegativeValue() {
//
//		int productIdInvalid = -1;
//
//		client.get()
//			.uri("/recommendation?productId=" + productIdInvalid)
//			.accept(APPLICATION_JSON)
//			.exchange()
//			.expectStatus().isEqualTo(UNPROCESSABLE_ENTITY)
//			.expectHeader().contentType(APPLICATION_JSON)
//			.expectBody()
//			.jsonPath("$.path").isEqualTo("/recommendation")
//			.jsonPath("$.message").isEqualTo("Invalid productId: " + productIdInvalid);
//	}

	private WebTestClient.BodyContentSpec getAndVerifyRecommendationsByProductId(int productId, HttpStatus expectedStatus) {
		return getAndVerifyRecommendationsByProductId("?productId=" + productId, expectedStatus);
	}

	private WebTestClient.BodyContentSpec getAndVerifyRecommendationsByProductId(String productIdQuery, HttpStatus expectedStatus) {
		return client.get()
			.uri("/recommendation" + productIdQuery)
			.accept(APPLICATION_JSON)
			.exchange()
			.expectStatus().isEqualTo(expectedStatus)
			.expectHeader().contentType(APPLICATION_JSON)
			.expectBody();
	}
//
//	private WebTestClient.BodyContentSpec postAndVerifyRecommendation(int productId, int recommendationId, HttpStatus status) {
//		Recommendation recommendation = new Recommendation(productId, recommendationId,
//			"Author " + recommendationId, recommendationId,
//			"Content " + recommendationId, "SA");
//
//		return client.post()
//			.uri("/recommendation")
//			.body(Mono.just(recommendation), Recommendation.class)
//			.accept(APPLICATION_JSON)
//			.exchange()
//			.expectStatus().isEqualTo(status)
//			.expectHeader().contentType(APPLICATION_JSON)
//			.expectBody();
//	}

	private WebTestClient.BodyContentSpec deleteAndVerifyRecommendationsByProductId(int productId, HttpStatus expectedStatus) {
		return client.delete()
			.uri("/recommendation?productId=" + productId)
			.accept(APPLICATION_JSON)
			.exchange()
			.expectStatus().isEqualTo(expectedStatus)
			.expectBody();
	}

	private void sendCreateRecommendationEvent(int productId, int recommendationId) {
		Recommendation recommendation = new Recommendation(productId, recommendationId,
			"Author " + recommendationId,
			recommendationId, "Content " + recommendationId, "SA");
		Event<Integer, Recommendation> event = new Event<>(Type.CREATE,
			productId, recommendation);
		input.send(new GenericMessage<>(event));
	}

	private void sendDeleteRecommendationEvent(int productId) {
		Event<Integer, Product> event = new Event(Type.DELETE, productId, null);
		input.send(new GenericMessage<>(event));
	}
}
