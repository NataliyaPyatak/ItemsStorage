# ===== ЭТАП 1: Сборка приложения =====
FROM maven:3.9-eclipse-temurin-21 AS build
WORKDIR /app

# Сначала копируем только pom.xml и качаем зависимости — это кешируемый слой
COPY pom.xml .
COPY .mvn/ .mvn/
COPY mvnw .
RUN ./mvnw dependency:go-offline -B

# Затем копируем исходники и собираем
COPY src ./src
RUN ./mvnw clean package -DskipTests

# ===== ЭТАП 2: Запуск =====
FROM eclipse-temurin:21-jre-alpine
WORKDIR /app

# Создаём непривилегированного пользователя (безопасность)
RUN addgroup -S spring && adduser -S spring -G spring
USER spring:spring

# Копируем собранный JAR из первого этапа
COPY --from=build /app/target/*.jar app.jar

# Spring Boot по умолчанию использует порт 8080, а у меня 8181 прописано
EXPOSE 8080

# Запускаем приложение
ENTRYPOINT ["java", "-Djava.security.egd=file:/dev/./urandom", "-jar", "app.jar"]