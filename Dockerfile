FROM eclipse-temurin:17-jdk-alpine
WORKDIR /app
COPY target/*.jar kitetradingbot-1.0.jar
EXPOSE 8080
ENTRYPOINT ["java","-jar","/app/kitetradingbot-1.0.jar"]