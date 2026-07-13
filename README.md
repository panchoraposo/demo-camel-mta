# demo-camel-mta

Side-by-side modernization demo using **Migration Toolkit for Applications (MTA)**:

- **`legacy/`**: Podman-first app stack based on **Red Hat build of Quarkus 2.13** + **Red Hat build of Camel (Camel Quarkus) 3**, integrated with **Red Hat AMQ Broker 7.x**.
- **`modern/`**: OpenShift-first app stack based on **Red Hat build of Quarkus 3.33** + **Red Hat build of Camel (Camel Quarkus) 4**, deployed via **OpenShift GitOps (Argo CD)**, and integrated with **OpenShift Logging** and **OpenShift Dev Spaces**.

Both stacks include:
- `queue-producer`: sends demo events to AMQ Broker
- `event-consumer`: consumes events and exposes `GET /api/snapshot`
- `visualizer`: minimal UI that polls snapshot endpoints to show live counters and last messages

## Documentation

See `docs/` (English):
- `docs/demo-script.md`
- `docs/run-local-legacy.md`
- `docs/run-mta-analysis.md`
- `docs/bootstrap-openshift-gitops.md`
- `docs/architecture.md`
- `docs/troubleshooting.md`

