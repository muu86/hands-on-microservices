package com.mj.microservices.core.product.persistence;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.mj.api.core.product.Product;
import java.util.List;
import java.util.Optional;
import java.util.stream.IntStream;
import org.assertj.core.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.data.mongo.DataMongoTest;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.dao.OptimisticLockingFailureException;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort.Direction;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.context.junit.jupiter.SpringExtension;

@DataMongoTest(properties = {"spring.data.mongodb.port=0",
    "spring.mongodb.embedded.version=3.6.9",
    "spring.data.mongodb.auto-index-creation=true"})
//@ExtendWith(SpringExtension.class)
//@TestPropertySource(properties = "spring.mongodb.embedded.version=3.4.7")
class ProductRepositoryTest {

    @Autowired
    private ProductRepository repository;

    private ProductEntity savedEntity;

    @BeforeEach
    public void setupDb() {
        repository.deleteAll();

        ProductEntity entity = new ProductEntity(1, "n", 1);
        savedEntity = repository.save(entity);

        assertThat(entity).isEqualTo(savedEntity);
    }

    @Test
    public void create() {
        ProductEntity entity = new ProductEntity(2, "n", 2);
        repository.save(entity);

        ProductEntity foundEntity = repository.findById(entity.getId()).get();

        assertEqualsProduct(entity, foundEntity);
        assertThat(repository.count()).isEqualTo(2);
    }

    @Test
    public void update() {
        savedEntity.setName("n2");
        repository.save(savedEntity);

        ProductEntity foundEntity = repository.findById(savedEntity.getId()).get();

        assertThat(foundEntity.getVersion()).isEqualTo(1);
        assertThat(foundEntity.getName()).isEqualTo("n2");
    }

    @Test
    public void delete() {
        repository.delete(savedEntity);

        assertThat(repository.existsById(savedEntity.getId())).isFalse();
    }

    @Test
    public void getByProductId() {
        Optional<ProductEntity> entity = repository.findByProductId(
            savedEntity.getProductId());

        assertThat(entity.isPresent()).isTrue();
        assertEqualsProduct(savedEntity, entity.get());
    }

    @Test
    public void duplicateError() {
//        assertThatThrownBy(() -> {
//            ProductEntity entity = new ProductEntity(savedEntity.getProductId(), "n", 1);
//            repository.save(entity);
//
//        }).isInstanceOf(DuplicateKeyException.class);
        System.out.println(savedEntity.getProductId());
        ProductEntity duplicate = new ProductEntity(savedEntity.getProductId(), "n", 1);
        System.out.println(duplicate);
        org.junit.jupiter.api.Assertions.assertThrows(DuplicateKeyException.class, () -> {
            ProductEntity entity = new ProductEntity(savedEntity.getProductId(), "n", 1);
            repository.save(duplicate);
        });
    }

    @Test
    public void optimisticLockError() {
        ProductEntity entity1 = repository.findById(savedEntity.getId()).get();
        ProductEntity entity2 = repository.findById(savedEntity.getId()).get();

        entity1.setName("n1");
        repository.save(entity1);

        assertThatThrownBy(() -> {
            entity2.setName("n2");
            repository.save(entity2);
        }).isInstanceOf(OptimisticLockingFailureException.class);

        ProductEntity updated = repository.findByProductId(savedEntity.getProductId()).get();
        assertThat(updated.getVersion()).isEqualTo(1);
        assertThat(updated.getName()).isEqualTo("n1");
    }

    @Test
    public void paging() {
        repository.deleteAll();

        List<ProductEntity> products = IntStream.rangeClosed(1001, 1010)
            .mapToObj(i -> new ProductEntity(i, "name_" + i, i)).toList();
        repository.saveAll(products);

        Pageable nextPage = PageRequest.of(0, 4, Direction.ASC, "productId");
        nextPage = testNextPage(nextPage, "[1001, 1002, 1003, 1004]", true);
        nextPage = testNextPage(nextPage, "[1005, 1006, 1007, 1008]", true);
        nextPage = testNextPage(nextPage, "[1009, 1010]", false);
    }

    private Pageable testNextPage(Pageable nextPage, String expectedProductIds,
        boolean expectsNextPage) {
        Page<ProductEntity> productPage = repository.findAll(nextPage);

        assertThat(expectedProductIds)
            .isEqualTo(
                productPage.getContent().stream()
                    .map(p -> p.getProductId()).toList().toString());
        assertThat(expectsNextPage).isEqualTo(productPage.hasNext());

        return productPage.nextPageable();
    }

    private void assertEqualsProduct(ProductEntity expectedEntity, ProductEntity actualEntity) {
        assertThat(expectedEntity.getId()).isEqualTo(actualEntity.getId());
        assertThat(expectedEntity.getVersion()).isEqualTo(actualEntity.getVersion());
        assertThat(expectedEntity.getProductId()).isEqualTo(actualEntity.getProductId());
        assertThat(expectedEntity.getName()).isEqualTo(actualEntity.getName());
        assertThat(expectedEntity.getWeight()).isEqualTo(actualEntity.getWeight());
    }
}