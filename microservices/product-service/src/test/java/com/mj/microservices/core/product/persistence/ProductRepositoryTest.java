package com.mj.microservices.core.product.persistence;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.data.mongo.DataMongoTest;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.dao.OptimisticLockingFailureException;
import reactor.test.StepVerifier;

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
        StepVerifier.create(repository.deleteAll())
            .verifyComplete();

        ProductEntity entity = new ProductEntity(1, "n", 1);

        StepVerifier.create(repository.save(entity))
            .expectNextMatches(createdEntity -> {
                savedEntity = createdEntity;
                return areProductEqual(entity, savedEntity);
            })
                .verifyComplete();
    }

    //    @Test
//    public void create() {
//        ProductEntity entity = new ProductEntity(2, "n", 2);
//        repository.save(entity);
//
//        ProductEntity foundEntity = repository.findById(entity.getId()).get();
//
//        assertEqualsProduct(entity, foundEntity);
//        assertThat(repository.count()).isEqualTo(2);
//    }
    @Test
    public void create_reactive() {
        ProductEntity newEntity = new ProductEntity(2, "n", 2);

        StepVerifier.create(repository.save(newEntity))
            .expectNextMatches(
                createdEntity -> createdEntity.getProductId() == newEntity.getProductId())
            .verifyComplete();

        StepVerifier.create(repository.findById(newEntity.getId()))
            .expectNextMatches(foundEntity -> areProductEqual(newEntity, foundEntity))
            .verifyComplete();

        StepVerifier.create(repository.count())
            .expectNext(2L)
            .verifyComplete();
    }

//    @Test
//    public void update() {
//        savedEntity.setName("n2");
//        repository.save(savedEntity);
//
//        ProductEntity foundEntity = repository.findById(savedEntity.getId()).get();
//
//        assertThat(foundEntity.getVersion()).isEqualTo(1);
//        assertThat(foundEntity.getName()).isEqualTo("n2");
//    }

    @Test
    public void update_reactive() {
        savedEntity.setName("n2");
        StepVerifier.create(repository.save(savedEntity))
            .expectNextMatches(updatedEntity -> updatedEntity.getName().equals("n2"))
            .verifyComplete();

        StepVerifier.create(repository.findById(savedEntity.getId()))
            .expectNextMatches(foundEntity ->
                foundEntity.getVersion() == 1 &&
                foundEntity.getName().equals("n2"))
            .verifyComplete();
    }

//    @Test
//    public void delete() {
//        repository.delete(savedEntity);
//
//        assertThat(repository.existsById(savedEntity.getId())).isFalse();
//    }

    @Test
    public void delete_reactive() {
        StepVerifier.create(repository.delete(savedEntity))
            .verifyComplete();

        StepVerifier.create(repository.existsById(savedEntity.getId()))
            .expectNext(false)
            .verifyComplete();
    }

//    @Test
//    public void getByProductId() {
//        Optional<ProductEntity> entity = repository.findByProductId(
//            savedEntity.getProductId());
//
//        assertThat(entity.isPresent()).isTrue();
//        assertEqualsProduct(savedEntity, entity.get());
//    }

    @Test
    public void getByProductId() {
        StepVerifier.create(repository.findByProductId(savedEntity.getProductId()))
            .expectNextMatches(foundEntity -> areProductEqual(foundEntity, savedEntity))
            .verifyComplete();
    }

//    @Test
//    public void duplicateError() {
////        assertThatThrownBy(() -> {
////            ProductEntity entity = new ProductEntity(savedEntity.getProductId(), "n", 1);
////            repository.save(entity);
////
////        }).isInstanceOf(DuplicateKeyException.class);
//        System.out.println(savedEntity.getProductId());
//        ProductEntity duplicate = new ProductEntity(savedEntity.getProductId(), "n", 1);
//        System.out.println(duplicate);
//        org.junit.jupiter.api.Assertions.assertThrows(DuplicateKeyException.class, () -> {
//            ProductEntity entity = new ProductEntity(savedEntity.getProductId(), "n", 1);
//            repository.save(duplicate);
//        });
//    }

    @Test
    public void duplicateError_reactive() {
        ProductEntity entity = new ProductEntity(savedEntity.getProductId(), "n", 1);
        StepVerifier.create(repository.save(entity))
            .expectError(DuplicateKeyException.class)
            .verify();
    }

//    @Test
//    public void optimisticLockError() {
//        ProductEntity entity1 = repository.findById(savedEntity.getId()).get();
//        ProductEntity entity2 = repository.findById(savedEntity.getId()).get();
//
//        entity1.setName("n1");
//        repository.save(entity1);
//
//        assertThatThrownBy(() -> {
//            entity2.setName("n2");
//            repository.save(entity2);
//        }).isInstanceOf(OptimisticLockingFailureException.class);
//
//        ProductEntity updated = repository.findByProductId(savedEntity.getProductId()).get();
//        assertThat(updated.getVersion()).isEqualTo(1);
//        assertThat(updated.getName()).isEqualTo("n1");
//    }

    @Test
    public void optimisticLockError_reactive() {
        ProductEntity entity1 = repository.findById(savedEntity.getId()).block();
        ProductEntity entity2 = repository.findById(savedEntity.getId()).block();

        entity1.setName("n1");
        repository.save(entity1).block();

        StepVerifier.create(repository.save(entity2))
            .expectError(OptimisticLockingFailureException.class)
            .verify();

        StepVerifier.create(repository.findById(savedEntity.getId()))
            .expectNextMatches(foundEntity ->
                foundEntity.getVersion() == 1 &&
                foundEntity.getName().equals("n1"))
            .verifyComplete();
    }

//    @Test
//    public void paging() {
//        repository.deleteAll();
//
//        List<ProductEntity> products = IntStream.rangeClosed(1001, 1010)
//            .mapToObj(i -> new ProductEntity(i, "name_" + i, i)).toList();
//        repository.saveAll(products);
//
//        Pageable nextPage = PageRequest.of(0, 4, Direction.ASC, "productId");
//        nextPage = testNextPage(nextPage, "[1001, 1002, 1003, 1004]", true);
//        nextPage = testNextPage(nextPage, "[1005, 1006, 1007, 1008]", true);
//        nextPage = testNextPage(nextPage, "[1009, 1010]", false);
//    }

//    private Pageable testNextPage(Pageable nextPage, String expectedProductIds,
//        boolean expectsNextPage) {
//        Page<ProductEntity> productPage = repository.findAll(nextPage);
//
//        assertThat(expectedProductIds)
//            .isEqualTo(
//                productPage.getContent().stream()
//                    .map(p -> p.getProductId()).toList().toString());
//        assertThat(expectsNextPage).isEqualTo(productPage.hasNext());
//
//        return productPage.nextPageable();
//    }

//    private void assertEqualsProduct(ProductEntity expectedEntity, ProductEntity actualEntity) {
//        assertThat(expectedEntity.getId()).isEqualTo(actualEntity.getId());
//        assertThat(expectedEntity.getVersion()).isEqualTo(actualEntity.getVersion());
//        assertThat(expectedEntity.getProductId()).isEqualTo(actualEntity.getProductId());
//        assertThat(expectedEntity.getName()).isEqualTo(actualEntity.getName());
//        assertThat(expectedEntity.getWeight()).isEqualTo(actualEntity.getWeight());
//    }

    private boolean areProductEqual(ProductEntity entity, ProductEntity savedEntity) {
        return (entity.getId().equals(savedEntity.getId()))
            && (entity.getProductId() == savedEntity.getProductId())
            && (entity.getVersion() == savedEntity.getVersion())
            && (entity.getName().equals(savedEntity.getName()))
            && (entity.getWeight() == savedEntity.getWeight());
    }
}