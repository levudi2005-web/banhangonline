FROM maven:3.9-eclipse-temurin-21 AS build
WORKDIR /build
COPY backend/pom.xml backend/pom.xml
RUN mvn -f backend/pom.xml -q -B dependency:go-offline
COPY backend/src backend/src
COPY frontend frontend
RUN mvn -f backend/pom.xml -q -B -DskipTests package

FROM eclipse-temurin:21-jre
RUN useradd -r -u 1001 app
WORKDIR /app
COPY --from=build /build/backend/target/app.jar app.jar
USER app
ENV JAVA_TOOL_OPTIONS="-XX:MaxRAMPercentage=75"
EXPOSE 8080
ENTRYPOINT ["java","-jar","app.jar"]
