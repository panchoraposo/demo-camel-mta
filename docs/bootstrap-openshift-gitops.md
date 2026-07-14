# Bootstrap OpenShift GitOps (Argo CD)

This repo uses an **app-of-apps** structure under `gitops/bootstrap/`.

## Prereqs

- OpenShift GitOps operator installed (one-time cluster bootstrap)
- Cluster-admin privileges (required to GitOps-install operators and cluster-scoped resources)

## Bootstrap

0) Install OpenShift GitOps Operator (one-time)

If the `openshift-gitops` namespace does not exist yet, install the operator first:

```bash
oc apply -f gitops/cluster-bootstrap/openshift-gitops-operator.yaml
```

Wait for the `openshift-gitops` namespace to be created and the Argo CD pods to become Ready.

0.1) (Demo convenience) allow Argo CD to manage cluster-scoped resources

For this demo we install operators and cluster-scoped resources with GitOps. Apply a one-time ClusterRoleBinding:

```bash
oc apply -f gitops/cluster-bootstrap/argocd-cluster-admin.yaml
```

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

