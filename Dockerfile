# -----------------------------------------------------------------------------
# Stage 1: Build stage với JDK 17
# -----------------------------------------------------------------------------
FROM eclipse-temurin:17-jdk-alpine AS builder
WORKDIR /app

# Copy gradle wrapper
COPY gradlew .
COPY gradle ./gradle

# SỬA DÒNG NÀY: Copy đúng 2 file .kts của dự án
COPY build.gradle.kts settings.gradle.kts ./

# Cấp quyền thực thi cho gradlew
RUN chmod +x gradlew

# Tải trước dependencies
RUN ./gradlew dependencies --no-daemon || true

# Copy toàn bộ mã nguồn
COPY src ./src

# Build file jar (bỏ qua test)
RUN ./gradlew bootJar --no-daemon -x test

# -----------------------------------------------------------------------------
# Stage 2: Runtime stage với JRE 17
# -----------------------------------------------------------------------------
FROM eclipse-temurin:17-jre-alpine
WORKDIR /app

RUN addgroup -S spring && adduser -S spring -G spring
USER spring:spring

COPY --from=builder /app/build/libs/*.jar app.jar

EXPOSE 8080

ENTRYPOINT ["java", "-XX:+UseContainerSupport", "-XX:MaxRAMPercentage=75.0", "-jar", "app.jar"]