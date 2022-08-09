package com.mj.microservices.composite.product;

import static org.mockito.Mockito.when;

import com.mj.api.composite.product.ProductAggregate;
import com.mj.api.composite.product.RecommendationSummary;
import com.mj.api.composite.product.ReviewSummary;
import com.mj.api.core.product.Product;
import com.mj.api.core.recommendation.Recommendation;
import com.mj.api.core.review.Review;
import com.mj.microservices.composite.product.services.ProductCompositeIntegration;
import com.mj.util.exceptions.InvalidInputException;
import com.mj.util.exceptions.NotFoundException;
import java.util.Collections;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.context.SpringBootTest.WebEnvironment;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.test.web.reactive.server.WebTestClient;
import reactor.core.publisher.Mono;

@SpringBootTest(webEnvironment = WebEnvironment.RANDOM_PORT)
class ProductCompositeServiceApplicationTests {

	private static final int PRODUCT_ID_OK = 1;
	private static final int PRODUCT_ID_NOT_FOUND = 2;
	private static final int PRODUCT_ID_INVALID = 3;

	@Autowired
	private WebTestClient client;

	@MockBean
	private ProductCompositeIntegration compositeIntegration;

	@BeforeEach
	public void setup() {
		when(compositeIntegration.getProduct(PRODUCT_ID_OK))
			.thenReturn(new Product(PRODUCT_ID_OK, "name", 1, "mock-address"));
		when(compositeIntegration.getRecommendations(PRODUCT_ID_OK))
			.thenReturn(Collections.singletonList(
				new Recommendation(PRODUCT_ID_OK, 1, "author", 1, "content", "mock address")));
		when(compositeIntegration.getReviews(PRODUCT_ID_OK))
			.thenReturn(Collections.singletonList(
				new Review(PRODUCT_ID_OK, 1, "author", "subject", "content", "mock address")));

		when(compositeIntegration.getProduct(PRODUCT_ID_NOT_FOUND))
			.thenThrow(new NotFoundException("NOT FOUND : " + PRODUCT_ID_NOT_FOUND));
		when(compositeIntegration.getProduct(PRODUCT_ID_INVALID))
			.thenThrow(new InvalidInputException("INVALID : " + PRODUCT_ID_INVALID));
	}

	@Test
	public void createCompositeProduct1() {
		ProductAggregate compositeProduct = new ProductAggregate(1, "name", 1, null, null, null);
		postAndVerifyProduct(compositeProduct, HttpStatus.OK);
	}

	@Test
	public void createCompositeProduct2() {
		ProductAggregate compositeProduct = new ProductAggregate(1, "name", 1,
			Collections.singletonList(new RecommendationSummary(1, "a", 1, "c")),
			Collections.singletonList(new ReviewSummary(1, "a", "s", "c")),
			null);
		postAndVerifyProduct(compositeProduct, HttpStatus.OK);
	}

	@Test
	public void deleteCompositeProduct() {
		ProductAggregate compositeProduct = new ProductAggregate(1, "name", 1,
			Collections.singletonList(new RecommendationSummary(1, "a", 1, "c")),
			Collections.singletonList(new ReviewSummary(1, "a", "s", "c")),
			null);

		postAndVerifyProduct(compositeProduct, HttpStatus.OK);

		deleteAndVerifyProduct(compositeProduct.getProductId(), HttpStatus.OK);
		deleteAndVerifyProduct(compositeProduct.getProductId(), HttpStatus.OK);
	}


	@Test
	public void getProductId() {
		client.get()
			.uri("/product-composite/" + PRODUCT_ID_OK)
			.accept(MediaType.APPLICATION_JSON)
			.exchange()
			.expectStatus().isOk()
			.expectHeader().contentType(MediaType.APPLICATION_JSON)
			.expectBody()
			.jsonPath("$.productId").isEqualTo(1)
			.jsonPath("$.recommendations.length()").isEqualTo(1)
			.jsonPath("$.reviews.length()").isEqualTo(1);
	}

	@Test
	public void getProductNotFound() {
		client.get()
			.uri("/product-composite/" + PRODUCT_ID_NOT_FOUND)
			.accept(MediaType.APPLICATION_JSON)
			.exchange()
			.expectStatus().isNotFound()
			.expectHeader().contentType(MediaType.APPLICATION_JSON)
			.expectBody()
			.jsonPath("$.path").isEqualTo("/product-composite/" + PRODUCT_ID_NOT_FOUND)
			.jsonPath("$.message").isEqualTo("NOT FOUND : " + PRODUCT_ID_NOT_FOUND);
	}

	@Test
	public void getProductInvalidInput() {
		client.get()
			.uri("/product-composite/" + PRODUCT_ID_INVALID)
			.accept(MediaType.APPLICATION_JSON)
			.exchange()
			.expectStatus().isEqualTo(HttpStatus.UNPROCESSABLE_ENTITY)
			.expectHeader().contentType(MediaType.APPLICATION_JSON)
			.expectBody()
			.jsonPath("$.path").isEqualTo("/product-composite/" + PRODUCT_ID_INVALID)
			.jsonPath("$.message").isEqualTo("INVALID : " + PRODUCT_ID_INVALID);
	}

	private void postAndVerifyProduct(ProductAggregate compositeProduct, HttpStatus status) {
		client.post()
			.uri("/product-composite")
			.body(Mono.just(compositeProduct), ProductAggregate.class)
			.exchange()
			.expectStatus().isEqualTo(status);
	}

	private void deleteAndVerifyProduct(int productId, HttpStatus status) {
		client.delete()
			.uri("/product-composite/" + productId)
			.exchange()
			.expectStatus().isEqualTo(status);
	}
}
