FROM openjdk:latest
MAINTAINER Devops guys
RUN mkdir webapps/
COPY target/bmp-product-inventory-business-service-1.0-SNAPSHOT.jar webapps/
ENTRYPOINT ["java","-jar","webapps/bmp-product-inventory-business-service-1.0-SNAPSHOT.jar"]
