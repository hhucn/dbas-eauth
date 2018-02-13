FROM openjdk:jre-alpine
ENV PORT 1236

COPY target/dbas.eauth-0.0.2-SNAPSHOT-standalone.jar dbas.eauth-0.0.2-SNAPSHOT-standalone.jar


EXPOSE $PORT
CMD ["java", "-jar", "dbas.eauth-0.0.2-SNAPSHOT-standalone.jar"]