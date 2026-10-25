# Multi-stage Dockerfile for Intelligent Search Engine
# Zero-dependency Java HTTP Server with 50,000-document LSA Semantic Search & Inverted Index

# Stage 1: Build Java Sources
FROM eclipse-temurin:21-jdk-alpine AS builder

WORKDIR /app

# Copy source directory
COPY src ./src

# Compile all Java sources into bytecode
RUN mkdir -p out && javac -encoding UTF-8 -sourcepath src -d out src/server/SearchServer.java

# Stage 2: Minimal Runtime Environment
FROM eclipse-temurin:21-jre-alpine

WORKDIR /app

# Copy compiled classes from build stage
COPY --from=builder /app/out ./out

# Copy dataset, semantic model matrices, and web frontend
COPY dataset ./dataset
COPY data ./data
COPY web ./web

# Copy seed state files
COPY analytics.json ./analytics.json
COPY google_evals.json ./google_evals.json
COPY workspace.json ./workspace.json

# Default port for local runs (Render injects $PORT at runtime)
ENV PORT=8080
EXPOSE 8080

# Launch server with memory-optimized JVM settings tailored for 512MB containers:
# - SerialGC eliminates G1GC region/card table memory overhead
# - Max heap 384m (75% of 512MB RAM), leaving 128MB for metaspace, thread stacks, and OS
CMD ["sh", "-c", "java -XX:+UseSerialGC -Xms128m -Xmx384m -XX:+ExitOnOutOfMemoryError -cp out server.SearchServer"]
