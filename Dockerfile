FROM eclipse-temurin:17-jdk-alpine
WORKDIR /app
COPY target/*.jar kitetradingbot-1.jar
ENTRYPOINT ["java","-jar","/app/kitetradingbot-1.jar"]