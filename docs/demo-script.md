# Demo script (MTA → Modernization → OpenShift GitOps)

This demo shows a simple messaging workload modernized from a Podman-first stack to OpenShift.

## 1) Show the legacy stack (Podman)

- Start AMQ Broker and the legacy apps:
  - `legacy/apps/queue-producer`
  - `legacy/apps/event-consumer`
  - `legacy/apps/visualizer`
- Open the legacy visualizer UI and generate a few messages.

## 2) Run MTA analysis on the legacy source

- Run an MTA CLI analysis report against `legacy/`.
- Highlight findings related to:
  - Quarkus 2 → Quarkus 3
  - Camel 3 → Camel 4
  - `javax.*` → `jakarta.*` namespace changes

## 3) Show the modern stack on OpenShift (GitOps)

- Bootstrap Argo CD root Application (app-of-apps).
- Show Operators being installed via GitOps:
  - AMQ Broker Operator
  - OpenShift Logging (+ Loki)
  - OpenShift Dev Spaces
- Show AMQ broker instance deployed via GitOps.
- Show modern apps deployed via GitOps:
  - producer / consumer / visualizer
- Open the modern visualizer Route and show live message counters.
- Increase the impact:
  - Scale the producer:
    - `oc -n demo-camel-mta scale deployment/modern-queue-producer --replicas=5`
  - Show how the **msg/s** and counters jump in the visualizer.
  - Optionally scale back down:
    - `oc -n demo-camel-mta scale deployment/modern-queue-producer --replicas=1`

## 4) Optional: Dev Spaces

- Start a Dev Spaces workspace using `modern/devfile.yaml`.
- Run `mvn -pl apps/event-consumer quarkus:dev` and show fast feedback loop.

## Notes for live demos

- The visualizer uses a **server-side snapshot proxy** (`GET /api/snapshots`) so it can call in-cluster services without exposing the consumer externally.
- OpenShift Logging (LokiStack) requires **object storage**; operator installation is GitOps-managed, but enabling the full log store is documented under `gitops/platform/logging/`.

