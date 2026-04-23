FROM europe-north1-docker.pkg.dev/cgr-nav/pull-through/nav.no/jre:openjdk-21@sha256:c20003ac820d32a8873f2c2a77c2b06786e7047c60b5fbe79f2c7531abbbce09
COPY build/libs/app.jar /app/app.jar
WORKDIR /app
USER nonroot
ENTRYPOINT ["java", "-jar", "app.jar"]
