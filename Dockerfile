# STAGE 1
FROM alpine/git as repo
WORKDIR apps/
ENV username
ENV password
ENV GIT_REPOSITORY
RUN git clone https://${username}:{$password}@github.com/mohammadaszadali/bmp-product-inventory-business-service.git
#STAGE 2
FROM maven:3.5-jdk-8-alpine as build
WORKDIR apps/
COPY --from=repo /apps/bmp-product-inventory-business-service apps/
RUN mvn clean install
#RUN mkdir -v apps
#COPY target/bmp-product-inventory-business-service-1.0-SNAPSHOT.jar apps/
#ENTRYPOINT ["java","-jar","apps/bmp-product-inventory-business-service.1.0-SNAPSHOT.jar"]
