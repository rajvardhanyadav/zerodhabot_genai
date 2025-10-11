FROM eclipse-temurin:17-jdk-alpine
WORKDIR /app
COPY target/*.jar tradingbot.jar
ENTRYPOINT ["java","-jar","/app/tradingbot.jar"]