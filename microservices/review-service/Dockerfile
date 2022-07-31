FROM openjdk:17.0.2

EXPOSE 8080

ADD ./build/libs/review-service-1.0.0-SNAPSHOT.jar app.jar

ENTRYPOINT ["java", "-jar", "/app.jar"]