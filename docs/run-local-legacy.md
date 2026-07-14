# Run legacy stack locally (Podman)

This runs **AMQ Broker 7.x** plus the **legacy** producer/consumer/visualizer apps on your workstation.

## Prereqs

- Podman
- Java + Maven (for source builds), or container build tools
- Access to Red Hat container registry (if you run AMQ Broker image from `registry.redhat.io`)

## Start

1) Login to Red Hat registry (needed for the broker image):

```bash
podman login registry.redhat.io
```

2) Build the legacy apps (host build, Java 17)

The legacy stack uses **RHBQ 2.x / Camel Quarkus 3**, so build it with **Java 17** on your machine:

```bash
export JAVA_HOME=$(/usr/libexec/java_home -v 17)

cd legacy
mvn -f pom.xml package -DskipTests
cd ..
```

3) Start the stack:

```bash
cd legacy/podman
podman compose up --build
```

4) Open:
- Producer API: `http://localhost:8080/api/send`
- Consumer snapshot: `http://localhost:8081/api/snapshot`
- Visualizer UI: `http://localhost:8082/`
- AMQ console: `http://localhost:8161/` (admin/admin)

