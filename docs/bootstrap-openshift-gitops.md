# Bootstrap OpenShift GitOps (Argo CD)

This repo uses an **app-of-apps** structure under `gitops/bootstrap/`.

## Prereqs

- OpenShift GitOps operator installed (one-time cluster bootstrap)
- Cluster-admin privileges (required to GitOps-install operators and cluster-scoped resources)

## Bootstrap

1) Apply the root Argo CD `Application`:

```bash
oc apply -f gitops/bootstrap/root-application.yaml
```

2) In the Argo CD UI, watch the sync order:
- `demo-camel-mta-operators`
- `demo-camel-mta-platform`
- `demo-camel-mta-apps`

3) Trigger builds (first time only):

```bash
oc -n demo-camel-mta start-build modern-event-consumer
oc -n demo-camel-mta start-build modern-queue-producer
oc -n demo-camel-mta start-build modern-visualizer
```

