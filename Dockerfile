# Imaginea Docker a aplicatiei: interfata React + backend-ul Spring Boot intr-un singur serviciu.
# Se construieste in 3 etape, ca imaginea finala sa contina doar Java si aplicatia (fara Node si Maven).

# 1. Interfata React -> fisiere statice (HTML/CSS/JS)
FROM node:22-alpine AS frontend
WORKDIR /app/frontend
COPY frontend/package.json frontend/package-lock.json ./
RUN npm ci
COPY frontend/ ./
RUN npm run build

# 2. Backend-ul Spring Boot, cu interfata inclusa in static/ (servita la aceeasi adresa ca API-ul)
FROM eclipse-temurin:21-jdk AS backend
WORKDIR /app
COPY .mvn .mvn
COPY mvnw pom.xml ./
RUN chmod +x mvnw && ./mvnw -q dependency:go-offline || true
COPY src src
COPY --from=frontend /app/frontend/dist src/main/resources/static
# Testele au nevoie de PostgreSQL, asa ca se ruleaza local, nu aici
RUN ./mvnw -q package -DskipTests

# 3. Imaginea finala: doar Java + aplicatia
FROM eclipse-temurin:21-jre-alpine
WORKDIR /app
COPY --from=backend /app/target/*.jar app.jar
# Java foloseste maxim 75% din memoria serverului (planul gratuit Render are 512 MB)
ENV JAVA_OPTS="-XX:MaxRAMPercentage=75"
EXPOSE 8080
ENTRYPOINT ["sh", "-c", "java $JAVA_OPTS -jar app.jar"]
