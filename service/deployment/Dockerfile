FROM eclipse-temurin:17-jre-alpine
VOLUME /tmp

# Low privileges user creation
RUN apk --no-cache add sudo
RUN apk --no-cache add curl
RUN sudo adduser --disabled-password -u 10111 appuser

# Container configuration
COPY init.sh .
COPY --chown=appuser:appuser app.jar .
EXPOSE 8080

# Service configuration
ENV JAVA_OPTS="-XX:+UseContainerSupport -Djava.security.egd=file:/dev/./urandom"

USER appuser
# ENTRYPOINT [ "sh", "-c", "java ${JAVA_OPTS} -jar ms-book-service.jar" ]