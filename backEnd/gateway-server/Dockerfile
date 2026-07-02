FROM eclipse-temurin:17-jre-alpine
VOLUME /tmp
COPY target/gateway-server-*.jar app.jar
ENTRYPOINT ["java","-jar","app.jar"]