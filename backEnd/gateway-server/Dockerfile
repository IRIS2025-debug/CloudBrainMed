FROM eclipse-temurin:17-jre-alpine
VOLUME /tmp
COPY target/gateway-server-*exec.jar app.jar
ENTRYPOINT ["java","-jar","app.jar"]