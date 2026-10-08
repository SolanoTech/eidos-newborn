# Самостоятельный модуль (без shared-library). Контекст сборки — папка модуля.
ARG JAVA_IMAGE=eclipse-temurin:26-jdk

FROM ${JAVA_IMAGE} AS build
COPY --from=maven:3.9.9-eclipse-temurin-21 /usr/share/maven /usr/share/maven
ENV MAVEN_HOME=/usr/share/maven
ENV PATH="/usr/share/maven/bin:${PATH}"
WORKDIR /app
COPY pom.xml .
RUN mvn -q -B -DskipTests dependency:go-offline || true
COPY src src
RUN mvn -q -B -DskipTests package

FROM ${JAVA_IMAGE}
WORKDIR /app
COPY --from=build /app/target/*.jar app.jar
EXPOSE 8083
ENTRYPOINT ["java", "-jar", "/app/app.jar"]
