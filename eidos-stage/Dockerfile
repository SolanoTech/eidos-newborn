# Контекст сборки — корень репозитория (нужен модуль shared-library).
ARG JAVA_IMAGE=eclipse-temurin:26-jdk

FROM ${JAVA_IMAGE} AS build
COPY --from=maven:3.9.9-eclipse-temurin-21 /usr/share/maven /usr/share/maven
ENV MAVEN_HOME=/usr/share/maven
ENV PATH="/usr/share/maven/bin:${PATH}"
WORKDIR /workspace

COPY shared-library/pom.xml shared-library/pom.xml
COPY shared-library/src shared-library/src
RUN mvn -q -B -DskipTests -f shared-library/pom.xml install

COPY eidos-stage/pom.xml eidos-stage/pom.xml
COPY eidos-stage/src eidos-stage/src
RUN mvn -q -B -DskipTests -f eidos-stage/pom.xml package

FROM ${JAVA_IMAGE}
WORKDIR /app
COPY --from=build /workspace/eidos-stage/target/*.jar app.jar
EXPOSE 8081
ENTRYPOINT ["java", "-jar", "/app/app.jar"]
