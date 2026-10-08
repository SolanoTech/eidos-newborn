# Контекст сборки — корень репозитория (нужен модуль shared-library).
# Maven подкладывается из официального образа в базовый образ JDK 26,
# чтобы не зависеть от наличия отдельного maven-образа под Java 26.
ARG JAVA_IMAGE=eclipse-temurin:26-jdk

FROM ${JAVA_IMAGE} AS build
COPY --from=maven:3.9.9-eclipse-temurin-21 /usr/share/maven /usr/share/maven
ENV MAVEN_HOME=/usr/share/maven
ENV PATH="/usr/share/maven/bin:${PATH}"
WORKDIR /workspace

# Сначала ставим shared-library в локальный репозиторий.
COPY shared-library/pom.xml shared-library/pom.xml
COPY shared-library/src shared-library/src
RUN mvn -q -B -DskipTests -f shared-library/pom.xml install

# Затем собираем сам сервис.
COPY eidos-core/pom.xml eidos-core/pom.xml
COPY eidos-core/src eidos-core/src
RUN mvn -q -B -DskipTests -f eidos-core/pom.xml package

FROM ${JAVA_IMAGE}
WORKDIR /app
COPY --from=build /workspace/eidos-core/target/*.jar app.jar
EXPOSE 8080
ENTRYPOINT ["java", "-jar", "/app/app.jar"]
