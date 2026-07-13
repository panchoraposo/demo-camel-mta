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

2) Start the stack:

```bash
cd legacy/podman
podman compose up --build
```

3) Open:
- Producer API: `http://localhost:8080/api/send`
- Consumer snapshot: `http://localhost:8081/api/snapshot`
- Visualizer UI: `http://localhost:8082/`
- AMQ console: `http://localhost:8161/` (admin/admin)

