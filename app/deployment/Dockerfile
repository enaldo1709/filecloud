FROM node:18.16.0-alpine
VOLUME /tmp

# Low privileges user creation
RUN apk --no-cache add sudo
RUN apk --no-cache add curl
RUN sudo adduser --disabled-password -u 10111 appuser

# Container configuration
COPY --chown=appuser:appuser app .
WORKDIR /app

RUN npm install

USER appuser
ENTRYPOINT [ "sh", "-c", "npm run start" ]