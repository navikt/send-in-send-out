FROM europe-north1-docker.pkg.dev/cgr-nav/pull-through/nav.no/jre:openjdk-21@sha256:87532692b9e5487d7d6ce5b3b8e1dc338daa07faeaa287b285823925b204b928
COPY build/libs/app.jar /app/app.jar
WORKDIR /app
USER nonroot
ENV JDK_JAVA_OPTIONS="-XX:MaxRAMPercentage=70.0"
ENTRYPOINT ["java", "-jar", "app.jar"]
