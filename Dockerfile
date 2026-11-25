FROM europe-north1-docker.pkg.dev/cgr-nav/pull-through/nav.no/jre:openjdk-21@sha256:042df23633aaa8df3bee8a9adac074ad61e41c389d95c8f10770a9bd83e5eab4
COPY build/libs/app.jar /app/app.jar
WORKDIR /app
USER nonroot
ENV JDK_JAVA_OPTIONS="-XX:MaxRAMPercentage=70.0"
ENTRYPOINT ["java", "-jar", "app.jar"]
