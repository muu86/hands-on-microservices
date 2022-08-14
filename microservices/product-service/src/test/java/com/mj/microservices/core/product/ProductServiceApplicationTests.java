package com.mj.microservices.core.product;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.fail;
import static org.springframework.boot.test.context.SpringBootTest.WebEnvironment.RANDOM_PORT;
import static org.springframework.http.MediaType.APPLICATION_JSON;
import static reactor.core.publisher.Mono.just;

import com.mj.api.core.product.Product;
import com.mj.api.event.Event;
import com.mj.api.event.Event.Type;
import com.mj.microservices.core.product.persistence.ProductRepository;
import com.mj.util.exceptions.InvalidInputException;
import lombok.extern.slf4j.Slf4j;
import org.assertj.core.api.Assertions;
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
import org.springframework.test.web.reactive.server.WebTestClient.BodyContentSpec;

@Slf4j
@SpringBootTest(webEnvironment = RANDOM_PORT,
	properties = { "spring.data.mongodb.port=0",
		"spring.mongodb.embedded.version=3.6.9",
		"spring.data.mongodb.auto-index-creation=true"
		})
public class ProductServiceApplicationTests {

	@Autowired
	private WebTestClient client;

	@Autowired
	private ProductRepository repository;

	@Autowired
	private Sink channels;

	private AbstractMessageChannel input = null;

	@BeforeEach
	public void setup() {
		log.info(channels.toString());
		input = (AbstractMessageChannel) channels.input();
		repository.deleteAll().block();
	}

	@Test
	public void getProductById() {

		int productId = 1;

		/* blocking-model-test
		postAndVerifyProduct(productId, OK);

		assertThat(repository.findByProductId(productId).isPresent()).isTrue();

		getAndVerifyProduct(productId, OK)
			.jsonPath("$.productId").isEqualTo(productId);
		*/

		assertThat(repository.findByProductId(productId).block()).isNull();
		assertThat(repository.count().block()).isEqualTo(0);

		sendCreateProductEvent(productId);
		assertThat(repository.findByProductId(productId).block()).isNotNull();
		assertThat(repository.count().block()).isEqualTo(1);

		getAndVerifyProduct(productId, HttpStatus.OK)
			.jsonPath("$.productId").isEqualTo(productId);
	}



	@Test
	public void duplicateError() {
		int productId = 1;

		/*
		postAndVerifyProduct(productId, OK);

		assertThat(repository.findByProductId(productId).isPresent()).isTrue();

		postAndVerifyProduct(productId, UNPROCESSABLE_ENTITY)
			.jsonPath("$.path").isEqualTo("/product")
			.jsonPath("$.message").isEqualTo("Duplicate Key, ProductId: " + productId);
		*/
		assertThat(repository.findByProductId(productId).block()).isNull();

		sendCreateProductEvent(productId);

		assertThat(repository.findByProductId(productId).block()).isNotNull();

		try {
			sendCreateProductEvent(productId);
			fail("Expected a MessagingException here!");
		} catch (MessagingException me) {
			if (me.getCause() instanceof InvalidInputException) {
				InvalidInputException iie = (InvalidInputException) me.getCause();
				assertThat(iie.getMessage()).isEqualTo("Duplicate key, Product Id: " + productId);
			} else {
				fail("Expected a InvalidInputException as the root cause!");
			}
		}
	}

	@Test
	public void deleteProduct() {
		int productId = 1;

		sendCreateProductEvent(productId);
		assertThat(repository.findByProductId(productId).block()).isNotNull();

		sendDeleteProductEvent(productId);
		assertThat(repository.findByProductId(productId).block()).isNull();

		sendDeleteProductEvent(productId);
	}

//	@Test
//	public void deleteProduct() {
//		int productId = 1;
//		postAndVerifyProduct(productId, OK);
//		assertThat(repository.findByProductId(productId).isPresent()).isTrue();
//
//		deleteAndVerifyProduct(productId, OK);
//		assertThat(repository.findByProductId(productId).isPresent()).isFalse();
//
////		deleteAndVerifyProduct(productId, OK);
//	}

	@Test
	public void getProductInvalidParameterString() {
/*
		log.info(new String(client.get()
				.uri("/product" + "/no-integer")
			.exchange()
			.expectBody()
			.returnResult()
			.getResponseBody()));

		getAndVerifyProduct("/no-integer", BAD_REQUEST)
			.jsonPath("$.path").isEqualTo("/product/no-integer");
//			.jsonPath("$.message").isEqualTo("Type mismatch.");
		*/

	}

//	@Test
//	public void getProductNotFound() {
//
//		int productIdNotFound = 13;
//
//		client.get()
//			.uri("/product/" + productIdNotFound)
//			.accept(APPLICATION_JSON)
//			.exchange()
//			.expectStatus().isNotFound()
//			.expectHeader().contentType(APPLICATION_JSON)
//			.expectBody()
//			.jsonPath("$.path").isEqualTo("/product/" + productIdNotFound)
//			.jsonPath("$.message").isEqualTo("No product found for productId: " + productIdNotFound);
//	}

//	@Test
//	public void getProductInvalidParameterNegativeValue() {
//
//		int productIdInvalid = -1;
//
//		client.get()
//			.uri("/product/" + productIdInvalid)
//			.accept(APPLICATION_JSON)
//			.exchange()
//			.expectStatus().isEqualTo(UNPROCESSABLE_ENTITY)
//			.expectHeader().contentType(APPLICATION_JSON)
//			.expectBody()
//			.jsonPath("$.path").isEqualTo("/product/" + productIdInvalid)
//			.jsonPath("$.message").isEqualTo("Invalid productId: " + productIdInvalid);
//	}

	private void sendCreateProductEvent(int productId) {
		Product product = new Product(productId, "Name " + productId, productId, "SA");
		Event<Integer, Product> event = new Event<>(Type.CREATE, productId, product);
		input.send(new GenericMessage<>(event));
	}

	private void sendDeleteProductEvent(int productId) {
		Event<Integer, Product> event = new Event(Type.DELETE, productId, null);
		input.send(new GenericMessage<>(event));
	}

	private BodyContentSpec getAndVerifyProduct(String productIdPath, HttpStatus status) {
		return client.get()
			.uri("/product" + productIdPath)
			.accept(APPLICATION_JSON)
			.exchange()
			.expectStatus().isEqualTo(status)
			.expectHeader().contentType(APPLICATION_JSON)
			.expectBody();
	}

	private BodyContentSpec getAndVerifyProduct(int productId, HttpStatus status) {
		return getAndVerifyProduct("/" + productId, status);
	}

	private WebTestClient.BodyContentSpec postAndVerifyProduct(int productId, HttpStatus status) {
		Product product = new Product(productId, "name_" + productId, productId, "SA");
		return client.post()
			.uri("/product")
			.body(just(product), Product.class)
			.accept(APPLICATION_JSON)
			.exchange()
			.expectStatus().isEqualTo(status)
			.expectHeader().contentType(APPLICATION_JSON)
			.expectBody();
	}

	private WebTestClient.BodyContentSpec deleteAndVerifyProduct(int productId, HttpStatus expectedStatus) {
		return client.delete()
			.uri("/product/" + productId)
			.accept(APPLICATION_JSON)
			.exchange()
			.expectStatus().isEqualTo(expectedStatus)
			.expectBody();
	}
}
