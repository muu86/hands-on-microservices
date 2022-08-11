package com.mj.microservices.core.product;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.springframework.boot.test.context.SpringBootTest.WebEnvironment.RANDOM_PORT;
import static org.springframework.http.HttpStatus.BAD_REQUEST;
import static org.springframework.http.HttpStatus.OK;
import static org.springframework.http.HttpStatus.UNPROCESSABLE_ENTITY;
import static org.springframework.http.MediaType.APPLICATION_JSON;
import static reactor.core.publisher.Mono.just;

import com.mj.api.core.product.Product;
import com.mj.microservices.core.product.persistence.ProductRepository;
import lombok.extern.slf4j.Slf4j;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.HttpStatus;
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

	@BeforeEach
	public void setup() {
		repository.deleteAll().block();
	}

//	@Test
//	public void getProductById() {
//
//		int productId = 1;
//
//		/* blocking-model-test
//		postAndVerifyProduct(productId, OK);
//
//		assertThat(repository.findByProductId(productId).isPresent()).isTrue();
//
//		getAndVerifyProduct(productId, OK)
//			.jsonPath("$.productId").isEqualTo(productId);
//		*/
//	}

//	@Test
//	public void duplicateError() {
//		int productId = 1;
//
//		postAndVerifyProduct(productId, OK);
//
//		assertThat(repository.findByProductId(productId).isPresent()).isTrue();
//
//		postAndVerifyProduct(productId, UNPROCESSABLE_ENTITY)
//			.jsonPath("$.path").isEqualTo("/product")
//			.jsonPath("$.message").isEqualTo("Duplicate Key, ProductId: " + productId);
//	}

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

//	@Test
//	public void getProductInvalidParameterString() {
//
//		log.info(new String(client.get()
//				.uri("/product" + "/no-integer")
//			.exchange()
//			.expectBody()
//			.returnResult()
//			.getResponseBody()));
//
//		getAndVerifyProduct("/no-integer", BAD_REQUEST)
//			.jsonPath("$.path").isEqualTo("/product/no-integer");
////			.jsonPath("$.message").isEqualTo("Type mismatch.");
//	}

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
