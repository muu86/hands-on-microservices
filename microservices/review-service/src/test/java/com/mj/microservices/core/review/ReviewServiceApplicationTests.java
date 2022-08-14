package com.mj.microservices.core.review;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.fail;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.springframework.boot.test.context.SpringBootTest.WebEnvironment.RANDOM_PORT;
import static org.springframework.http.HttpStatus.OK;
import static org.springframework.http.MediaType.APPLICATION_JSON;

import com.mj.api.core.review.Review;
import com.mj.api.event.Event;
import com.mj.api.event.Event.Type;
import com.mj.microservices.core.review.persistence.ReviewRepository;
import com.mj.util.exceptions.InvalidInputException;
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

@SpringBootTest(webEnvironment = RANDOM_PORT,
	properties = {
	"spring.jpa.properties.hibernate.dialect=org.hibernate.dialect.H2Dialect",
	"spring.datasource.driver-class-name=org.h2.Driver",
	"spring.datasource.url=jdbc:h2:mem:review-db" })
public class ReviewServiceApplicationTests {

	@Autowired
	private WebTestClient client;

	@Autowired
	private ReviewRepository repository;

	@Autowired
	private Sink channels;

	private AbstractMessageChannel input = null;

	@BeforeEach
	public void setupDb() {
		input = (AbstractMessageChannel) channels.input();
		repository.deleteAll();
	}

	@Test
	public void getReviewsByProductId() {

		int productId = 1;

		assertThat(repository.findByProductId(productId).size()).isEqualTo(0);

		sendCreateReviewEvent(productId, 1);
		sendCreateReviewEvent(productId, 2);
		sendCreateReviewEvent(productId, 3);

		assertEquals(3, repository.findByProductId(productId).size());

		getAndVerifyReviewsByProductId(productId, OK)
			.jsonPath("$.length()").isEqualTo(3)
			.jsonPath("$[2].productId").isEqualTo(productId)
			.jsonPath("$[2].reviewId").isEqualTo(3);
	}

	@Test
	public void duplicateError() {

		int productId = 1;
		int reviewId = 1;

		assertEquals(0, repository.count());

		sendCreateReviewEvent(productId, reviewId);

		assertEquals(1, repository.count());

		try {
			sendCreateReviewEvent(productId, reviewId);
			fail("Expected a MessagingException here!");
		} catch (MessagingException me) {
			if (me.getCause() instanceof InvalidInputException)	{
				InvalidInputException iie = (InvalidInputException)me.getCause();
				assertEquals("Duplicate key, Product Id: 1, Review Id:1", iie.getMessage());
			} else {
				fail("Expected a InvalidInputException as the root cause!");
			}
		}

		assertEquals(1, repository.count());
	}

	@Test
	public void deleteReviews() {

		int productId = 1;
		int reviewId = 1;

		sendCreateReviewEvent(productId, reviewId);
		assertEquals(1, repository.findByProductId(productId).size());

		sendDeleteReviewEvent(productId);
		assertEquals(0, repository.findByProductId(productId).size());

		sendDeleteReviewEvent(productId);
	}

//	@Test
//	public void getReviewsByProductId() {
//
//		int productId = 1;
//
//		client.get()
//			.uri("/review?productId=" + productId)
//			.accept(APPLICATION_JSON)
//			.exchange()
//			.expectStatus().isOk()
//			.expectHeader().contentType(APPLICATION_JSON)
//			.expectBody()
//			.jsonPath("$.length()").isEqualTo(3)
//			.jsonPath("$[0].productId").isEqualTo(productId);
//	}
//
//	@Test
//	public void getReviewsMissingParameter() {
//
//		client.get()
//			.uri("/review")
//			.accept(APPLICATION_JSON)
//			.exchange()
//			.expectStatus().isEqualTo(BAD_REQUEST)
//			.expectHeader().contentType(APPLICATION_JSON)
//			.expectBody()
//			.jsonPath("$.path").isEqualTo("/review")
//			.jsonPath("$.message").isEqualTo("Required int parameter 'productId' is not present");
//	}
//
//	@Test
//	public void getReviewsInvalidParameter() {
//
//		client.get()
//			.uri("/review?productId=no-integer")
//			.accept(APPLICATION_JSON)
//			.exchange()
//			.expectStatus().isEqualTo(BAD_REQUEST)
//			.expectHeader().contentType(APPLICATION_JSON)
//			.expectBody()
//			.jsonPath("$.path").isEqualTo("/review")
//			.jsonPath("$.message").isEqualTo("Type mismatch.");
//	}
//
//	@Test
//	public void getReviewsNotFound() {
//
//		int productIdNotFound = 213;
//
//		client.get()
//			.uri("/review?productId=" + productIdNotFound)
//			.accept(APPLICATION_JSON)
//			.exchange()
//			.expectStatus().isOk()
//			.expectHeader().contentType(APPLICATION_JSON)
//			.expectBody()
//			.jsonPath("$.length()").isEqualTo(0);
//	}
//
//	@Test
//	public void getReviewsInvalidParameterNegativeValue() {
//
//		int productIdInvalid = -1;
//
//		client.get()
//			.uri("/review?productId=" + productIdInvalid)
//			.accept(APPLICATION_JSON)
//			.exchange()
//			.expectStatus().isEqualTo(UNPROCESSABLE_ENTITY)
//			.expectHeader().contentType(APPLICATION_JSON)
//			.expectBody()
//			.jsonPath("$.path").isEqualTo("/review")
//			.jsonPath("$.message").isEqualTo("Invalid productId: " + productIdInvalid);
//	}

	private WebTestClient.BodyContentSpec getAndVerifyReviewsByProductId(int productId, HttpStatus expectedStatus) {
		return getAndVerifyReviewsByProductId("?productId=" + productId, expectedStatus);
	}

	private WebTestClient.BodyContentSpec getAndVerifyReviewsByProductId(String productIdQuery, HttpStatus expectedStatus) {
		return client.get()
			.uri("/review" + productIdQuery)
			.accept(APPLICATION_JSON)
			.exchange()
			.expectStatus().isEqualTo(expectedStatus)
			.expectHeader().contentType(APPLICATION_JSON)
			.expectBody();
	}

	private void sendCreateReviewEvent(int productId, int reviewId) {
		Review review = new Review(productId, reviewId, "Author " + reviewId, "Subject " + reviewId, "Content " + reviewId, "SA");
		Event<Integer, Review> event = new Event<>(Type.CREATE, productId, review);
		input.send(new GenericMessage<>(event));
	}

	private void sendDeleteReviewEvent(int productId) {
		Event<Integer, Review> event = new Event(Type.DELETE, productId, null);
		input.send(new GenericMessage<>(event));
	}
}
