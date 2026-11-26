FROM europe-north1-docker.pkg.dev/cgr-nav/pull-through/nav.no/jre:openjdk-21@sha256:873ece75285560547de79eab5973df19b20770204e92152a26fc7c762bd138c6
COPY build/libs/app.jar /app/app.jar
WORKDIR /app
USER nonroot
ENV JDK_JAVA_OPTIONS="-XX:MaxRAMPercentage=70.0"
ENTRYPOINT ["java", "-jar", "app.jar"]
