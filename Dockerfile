FROM europe-north1-docker.pkg.dev/cgr-nav/pull-through/nav.no/jre:openjdk-21@sha256:406e6b586ba79b4b1b0f4670c0df9467304dc06427b1f6ddc242537914b0c869
COPY build/libs/app.jar /app/app.jar
WORKDIR /app
USER nonroot
ENV JDK_JAVA_OPTIONS="-XX:MaxRAMPercentage=70.0"
ENTRYPOINT ["java", "-jar", "app.jar"]
