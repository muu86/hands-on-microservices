package com.mj.api.event;

import java.util.Date;
import lombok.Getter;

@Getter
public class Event<K, T> {

    public enum Type { CREATE, DELETE }

    private Event.Type eventType;
    private K key;
    private T data;
    private Date eventCreatedAt;

    public Event(Type eventType) {
        this.eventType = null;
        this.key = null;
        this.data = null;
        this.eventCreatedAt = null;
    }

    public Event(Type eventType, K key, T data) {
        this.eventType = eventType;
        this.key = key;
        this.data = data;
        this.eventCreatedAt = new Date();
    }
}
