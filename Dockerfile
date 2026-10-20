# 公開用のイメージ。ビルド用(Maven)と実行用(JRE だけ)を分けて、実行イメージを小さくする。
FROM maven:3.9-eclipse-temurin-17 AS build
WORKDIR /app
COPY pom.xml .
RUN mvn -B -q dependency:go-offline
COPY src ./src
# テストは GitHub Actions で実行済みなので、ここでは省く
RUN mvn -B -q package -DskipTests && cp target/wordfall-*.jar app.jar

FROM eclipse-temurin:17-jre
WORKDIR /app
RUN useradd --system --no-create-home wordfall
COPY --from=build /app/app.jar app.jar
USER wordfall
ENV SPRING_PROFILES_ACTIVE=prod \
    JAVA_TOOL_OPTIONS="-XX:MaxRAMPercentage=75"
EXPOSE 8080
ENTRYPOINT ["java", "-jar", "app.jar"]
