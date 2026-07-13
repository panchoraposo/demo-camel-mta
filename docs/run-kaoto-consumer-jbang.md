# Run Kaoto consumer route with JBang (IDE-friendly)

This demo includes a **Kaoto-friendly Camel route** in YAML DSL:

- `modern/apps/kaoto-consumer/src/main/resources/routes/kaoto-consumer.camel.yaml`

You can edit this file visually with **Kaoto** (in OpenShift Dev Spaces) and also run it locally using **Camel JBang**.

## Prereqs

- Java 17+ installed locally
- `jbang` installed
- Access to the AMQ Broker (local or OpenShift)

## Run the YAML route with Camel JBang

From the repository root:

```bash
cd modern/apps/kaoto-consumer/src/main/resources/routes

# Run the route (Camel JBang will download its runtime)
jbang -q camel@apache/camel run kaoto-consumer.camel.yaml \
  --repos=https://maven.repository.redhat.com/ga \
  --properties=demo.broker.url=tcp://localhost:61616 \
  --properties=demo.broker.username=admin \
  --properties=demo.broker.password=admin \
  --properties=demo.broker.queue=demo.events \
  --properties=demo.enhanced.logging.enabled=true \
  --deps=org.apache.activemq:artemis-jakarta-client:2.39.0
```

Notes:

- We pass `--repos=https://maven.repository.redhat.com/ga` to prefer Red Hat artifacts where available.
- The `--deps` line adds the Artemis Jakarta JMS client needed by `camel-jms`.

## Run in Dev Spaces (Quarkus dev mode)

In your Dev Spaces workspace, run the devfile command:

```bash
mvn -f modern/pom.xml -pl apps/kaoto-consumer quarkus:dev
```

Then open:

- `http://localhost:8083/api/snapshot` (in Dev Spaces: use port-forwarding / route as appropriate)

