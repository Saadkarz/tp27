# ===========================================
# Dockerfile Multi-Stage pour Book Service
# TP27 - Test de Concurrence & Résilience
# ===========================================

# Stage 1: Build avec Maven
FROM maven:3.9-eclipse-temurin-17 AS builder

WORKDIR /app

# Copier les fichiers POM et télécharger les dépendances (cache layer)
COPY pom.xml .
RUN mvn dependency:go-offline -B

# Copier le code source et construire
COPY src ./src
RUN mvn clean package -DskipTests -B

# Stage 2: Runtime avec JRE léger
FROM eclipse-temurin:17-jre-alpine

WORKDIR /app

# Créer un utilisateur non-root pour la sécurité
RUN addgroup -g 1001 -S appgroup && \
    adduser -u 1001 -S appuser -G appgroup

# Copier le JAR depuis le stage de build
COPY --from=builder /app/target/book-service.jar app.jar

# Changer le propriétaire du fichier
RUN chown appuser:appgroup app.jar

# Utiliser l'utilisateur non-root
USER appuser

# Port exposé
EXPOSE 8080

# Variables d'environnement par défaut
ENV JAVA_OPTS="-Xmx512m -Xms256m"
ENV SPRING_PROFILES_ACTIVE=prod

# Health check
HEALTHCHECK --interval=30s --timeout=10s --start-period=90s --retries=5 \
    CMD wget --no-verbose --tries=1 --spider http://localhost:8080/actuator/health || exit 1

# Point d'entrée
ENTRYPOINT ["sh", "-c", "java $JAVA_OPTS -jar app.jar"]
