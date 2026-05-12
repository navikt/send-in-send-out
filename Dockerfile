FROM europe-north1-docker.pkg.dev/cgr-nav/pull-through/nav.no/jre:openjdk-21@sha256:a6b60dde31b214c2f1f85e1cce19c690815c14dd6fa8fef8b9ebc3f9bd4c95e0
COPY build/libs/app.jar /app/app.jar
WORKDIR /app
USER nonroot
ENTRYPOINT ["java", "-jar", "app.jar"]
