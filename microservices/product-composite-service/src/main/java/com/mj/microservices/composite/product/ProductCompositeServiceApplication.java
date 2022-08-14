package com.mj.microservices.composite.product;

import com.mj.microservices.composite.product.services.ProductCompositeIntegration;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.actuate.health.CompositeReactiveHealthContributor;
import org.springframework.boot.actuate.health.DefaultHealthContributorRegistry;
import org.springframework.boot.actuate.health.DefaultReactiveHealthContributorRegistry;
import org.springframework.boot.actuate.health.Health;
import org.springframework.boot.actuate.health.ReactiveHealthContributor;
import org.springframework.boot.actuate.health.ReactiveHealthContributorRegistry;
import org.springframework.boot.actuate.health.ReactiveHealthIndicator;
import org.springframework.boot.actuate.health.StatusAggregator;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.http.HttpMethod;
import org.springframework.integration.annotation.Reactive;
import org.springframework.web.client.RestTemplate;
import reactor.core.publisher.Mono;
import springfox.documentation.builders.PathSelectors;
import springfox.documentation.builders.RequestHandlerSelectors;
import springfox.documentation.service.ApiInfo;
import springfox.documentation.service.Contact;
import springfox.documentation.spi.DocumentationType;
import springfox.documentation.spring.web.plugins.Docket;

@SpringBootApplication
@ComponentScan("com.mj")
public class ProductCompositeServiceApplication {

	public static void main(String[] args) {
		SpringApplication.run(ProductCompositeServiceApplication.class, args);
	}

	@Value("${api.common.version}") String apiVersion;
	@Value("${api.common.title}") String apiTitle;
	@Value("${api.common.description}")       String apiDescription;
	@Value("${api.common.termsOfServiceUrl}") String apiTermsOfServiceUrl;
	@Value("${api.common.license}")           String apiLicense;
	@Value("${api.common.licenseUrl}")        String apiLicenseUrl;
	@Value("${api.common.contact.name}")      String apiContactName;
	@Value("${api.common.contact.url}")       String apiContactUrl;
	@Value("${api.common.contact.email}")     String apiContactEmail;

	@Bean
	public Docket apiDocumentation() {
		return new Docket(DocumentationType.SWAGGER_2)
			.select()
			.apis(RequestHandlerSelectors.basePackage("com.mj.microservices.composite.product"))
			.paths(PathSelectors.any())
			.build()
			.globalResponses(HttpMethod.GET, Collections.emptyList())
			.apiInfo(new ApiInfo(
				apiTitle,
				apiDescription,
				apiVersion,
				apiTermsOfServiceUrl,
				new Contact(apiContactName, apiContactUrl, apiContactEmail),
				apiLicense,
				apiLicenseUrl,
				Collections.emptyList()
			));
	}

	@Bean
	public RestTemplate restTemplate() {
		return new RestTemplate();
	}

	@Autowired
	StatusAggregator statusAggregator;

	@Autowired
	ProductCompositeIntegration integration;

	@Bean
	ReactiveHealthContributor coreServices() {
		return CompositeReactiveHealthContributor.fromMap(Map.of(
			"product", new ReactiveHealthIndicator() {
				@Override
				public Mono<Health> health() {
					return integration.getProductHealth();
				}
			},
			"recommendation", new ReactiveHealthIndicator() {
				@Override
				public Mono<Health> health() {
					return integration.getRecommendationHealth();
				}
			},
			"review", new ReactiveHealthIndicator() {
				@Override
				public Mono<Health> health() {
					return integration.getReviewHealth();
				}
			}));
	}
}
