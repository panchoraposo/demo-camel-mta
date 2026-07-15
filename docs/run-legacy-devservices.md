## Run legacy apps with Quarkus Dev Services (local)

This repo supports **two local options** for the legacy apps:

- **Podman Compose** (explicit local broker): uses the Red Hat AMQ Broker image and fixed ports.
- **Quarkus Dev Services** (automatic broker): starts an Artemis broker container automatically when you run `quarkus:dev`.

### Option A: Podman Compose (legacy apps + Red Hat AMQ Broker image)

Run the full legacy stack locally (broker + producer + consumer):

```bash
podman compose -f legacy/podman/compose.yaml up
```

This uses the Red Hat AMQ Broker image (`registry.redhat.io/amq7/...`) and the apps connect via `DEMO_BROKER_URL`.

### Option B: Quarkus Dev Services (legacy apps)

Quarkus Dev Services will start a broker container when:
- you run `quarkus:dev`, and
- you do **not** set `DEMO_BROKER_URL`

The legacy apps are configured to resolve broker URL in this order:
1) `DEMO_BROKER_URL` (if set)
2) `quarkus.artemis.url` (set automatically by Dev Services)
3) `tcp://localhost:61616` (fallback)

Start the legacy consumer in dev mode:

```bash
sh legacy/mvnw -f legacy/pom.xml -pl apps/event-consumer quarkus:dev
```

Start the legacy producer in dev mode (in another terminal):

```bash
sh legacy/mvnw -f legacy/pom.xml -pl apps/queue-producer quarkus:dev
```

#### Using the Red Hat broker image with Dev Services (best-effort)

Dev Services defaults to an ArtemisCloud image. You can **try** using the Red Hat AMQ Broker image by overriding:

```bash
DEMO_ARTEMIS_IMAGE=registry.redhat.io/amq7/amq-broker-rhel9:7.14 sh legacy/mvnw -f legacy/pom.xml -pl apps/event-consumer quarkus:dev
```

If the broker fails to start, unset `DEMO_ARTEMIS_IMAGE` to fall back to the default Dev Services image.

### Modern apps in dev mode, pointing to the demo broker

For the modern apps, prefer pointing dev mode to the **demo broker** instead of starting a local broker.
From your laptop you typically need a port-forward:

```bash
oc -n demo-camel-mta port-forward svc/amq-broker-core-0-svc 61616:61616
```

Then run a modern app with:

```bash
DEMO_BROKER_URL=tcp://localhost:61616 sh modern/mvnw -f modern/pom.xml -pl apps/queue-producer quarkus:dev
```

