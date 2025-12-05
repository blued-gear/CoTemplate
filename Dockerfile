FROM docker.io/library/alpine:latest as PULL

ARG GL_HOST
ARG GL_PROJECT_ID
ARG PACKAGE_VER

RUN apk update && \
    apk add curl

# change number to invalidate cached tar
RUN echo 1

RUN curl \
      "https://$GL_HOST/api/v4/projects/$GL_PROJECT_ID/packages/generic/cotemplate/$PACKAGE_VER/cotemplate-$PACKAGE_VER.tgz" \
      >app.tgz && \
    mkdir /app && \
    tar -C /app -xf app.tgz


FROM docker.io/library/eclipse-temurin:25-jdk-alpine

COPY --from=PULL --chown=1001:1001 /app/ /app

USER 1001
WORKDIR /app
CMD ["java", "-jar", "./quarkus-run.jar"]
