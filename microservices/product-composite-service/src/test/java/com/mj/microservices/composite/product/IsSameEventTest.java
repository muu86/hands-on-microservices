package com.mj.microservices.composite.product;

import static com.mj.microservices.composite.product.IsSameEvent.sameEventExceptCreatedAt;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.not;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.mj.api.core.product.Product;
import com.mj.api.event.Event;
import com.mj.api.event.Event.Type;
import org.junit.jupiter.api.Test;

class IsSameEventTest {

    ObjectMapper mapper = new ObjectMapper();

    @Test
    public void testEventObjectCompare() throws JsonProcessingException {
        Event<Integer, Product> event1 = new Event<>(Type.CREATE, 1, new Product(1, "name", 1, null));
        Event<Integer, Product> event2 = new Event<>(Type.CREATE, 1, new Product(1, "name", 1, null));
        Event<Integer, Product> event3 = new Event(Type.DELETE, 1, null);
        Event<Integer, Product> event4 = new Event<>(Type.CREATE, 1, new Product(2, "name", 1, null));

        String event1Json = mapper.writeValueAsString(event1);

        assertThat(event1Json, is(sameEventExceptCreatedAt(event2)));
        assertThat(event1Json, not(sameEventExceptCreatedAt(event3)));
        assertThat(event1Json, not(sameEventExceptCreatedAt(event4)));
    }
}