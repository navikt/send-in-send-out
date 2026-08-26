FROM europe-north1-docker.pkg.dev/cgr-nav/pull-through/nav.no/jre:openjdk-21@sha256:d2d6d676f72e9c937047adf74479e6c111bd20ef3a0d9e5f217e1522c289baad
COPY build/libs/app.jar /app/app.jar
WORKDIR /app
USER nonroot
ENTRYPOINT ["java", "-jar", "app.jar"]
