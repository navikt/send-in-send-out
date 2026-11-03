FROM europe-north1-docker.pkg.dev/cgr-nav/pull-through/nav.no/jre:openjdk-21@sha256:b3ba39f948d39ff68a08e4dbacfcf72457e2097c5740ac15a3d7aa175b3e3760
COPY build/libs/app.jar /app/app.jar
WORKDIR /app
USER nonroot
ENV JDK_JAVA_OPTIONS="-XX:MaxRAMPercentage=70.0"
ENTRYPOINT ["java", "-jar", "app.jar"]
