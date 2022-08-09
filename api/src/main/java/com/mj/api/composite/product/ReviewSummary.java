package com.mj.api.composite.product;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class ReviewSummary {

    private final int reviewId;
    private final String author;
    private final String subject;
    private final String content;

    @JsonCreator
    public ReviewSummary(
        @JsonProperty("reviewId") int reviewId,
        @JsonProperty("author") String author,
        @JsonProperty("subject") String subject,
        @JsonProperty("content") String content
    ) {
        this.reviewId = reviewId;
        this.author = author;
        this.subject = subject;
        this.content = content;
    }
}
