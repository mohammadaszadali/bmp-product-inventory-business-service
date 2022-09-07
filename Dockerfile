# STAGE 1
FROM alpine/git as repo
WORKDIR /apps
ARG username
ARG password
RUN git clone -b development https://${username}:${password}@github.com/mohammadaszadali/bmp-product-inventory-business-service.git
RUN pwd && ls -latr
#STAGE 2
FROM maven:3.5-jdk-8-alpine as build
WORKDIR apps/
COPY --from=repo /apps/bmp-product-inventory-business-service apps/bmp-product-inventory-business-service apps/
RUN  pwd && ls -latr && cd apps/ && mvn clean install
#STAGE 3
FROM openjdk:8
WORKDIR apps/
COPY --from=build /apps/apps/target/bmp-product-inventory-business-service-1.0-SNAPSHOT.jar apps/bmp-product-inventory-business-service-1.0-SNAPSHOT.jar
ENTRYPOINT ["java","-jar","apps/bmp-product-inventory-business-service-1.0-SNAPSHOT.jar"]
