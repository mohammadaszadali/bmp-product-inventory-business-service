FROM openjdk:8
RUN mkdir -v apps
COPY target/bmp-product-inventory-business-service-1.0-SNAPSHOT.jar apps/
ENTRYPOINT ["java","-jar","apps/bmp-product-inventory-business-service-1.0-SNAPSHOT.jar"]
