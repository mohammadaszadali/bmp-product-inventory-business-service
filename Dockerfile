# STAGE 1
FROM alpine/git as repo
WORKDIR apps/
ENV username "mohammadaszadali"
ENV password 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
ENV GIT_REPOSITORY https://github.com/mohammadaszadali/bmp-product-inventory-business-service.git
RUN git clone https://$username:$password@$GIT_REPOSITORY
#STAGE 2
FROM maven:3.5-jdk-8-alpine as build
WORKDIR apps/
COPY --from=repo /apps/bmp-product-inventory-business-service apps/
RUN mvn clean install
#RUN mkdir -v apps
#COPY target/bmp-product-inventory-business-service-1.0-SNAPSHOT.jar apps/
#ENTRYPOINT ["java","-jar","apps/bmp-product-inventory-business-service.1.0-SNAPSHOT.jar"]
