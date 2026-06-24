FROM eclipse-temurin:21-jre

RUN apt-get update \
    && apt-get install -y --no-install-recommends ca-certificates webp imagemagick \
    && rm -rf /var/lib/apt/lists/*

RUN useradd --system --uid 10001 --create-home app \
    && mkdir -p /app /tmp/pilgrimage-ingest \
    && chown -R app:app /app /tmp/pilgrimage-ingest

WORKDIR /app
COPY target/pilgrimage-0.0.1-SNAPSHOT.jar /app/app.jar

USER app
ENTRYPOINT ["java", "-jar", "/app/app.jar"]
