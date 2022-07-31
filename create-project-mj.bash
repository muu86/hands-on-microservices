#!/usr/bin/env bash

mkdir microservices
cd microservices

spring init \
--boot-version=2.7.2 \
--build=gradle \
--java-version=17 \
--packaging=jar \
--name=product-service \
--package-name=com.mj.microservices.core.product \
--groupId=com.mj.microservices.core.product \
--dependencies=actuator,webflux \
--version=1.0.0-SNAPSHOT \
product-service

spring init \
--boot-version=2.7.2 \
--build=gradle \
--java-version=17 \
--packaging=jar \
--name=review-service \
--package-name=com.mj.microservices.core.review \
--groupId=com.mj.microservices.core.review \
--dependencies=actuator,webflux \
--version=1.0.0-SNAPSHOT \
review-service

spring init \
--boot-version=2.7.2 \
--build=gradle \
--java-version=17 \
--packaging=jar \
--name=recommendation-service \
--package-name=com.mj.microservices.core.recommendation \
--groupId=com.mj.microservices.core.recommendation \
--dependencies=actuator,webflux \
--version=1.0.0-SNAPSHOT \
recommendation-service

spring init \
--boot-version=2.7.2 \
--build=gradle \
--java-version=17 \
--packaging=jar \
--name=product-composite-service \
--package-name=se.magnus.microservices.composite.product \
--groupId=se.magnus.microservices.composite.product \
--dependencies=actuator,webflux \
--version=1.0.0-SNAPSHOT \
product-composite-service

cd ..

