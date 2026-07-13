# Troubleshooting

## Maven cannot resolve Red Hat artifacts

- Ensure your network can reach `https://maven.repository.redhat.com/ga`.
- Retry with `mvn -U ...` to force dependency re-download.

## AMQ broker operator channel mismatch

The AMQ Broker operator uses versioned channels (for example `7.14.x`). If your cluster catalog does not offer that channel, adjust the Subscription under `gitops/operators/amq-broker/subscription.yaml`.

## OpenShift Logging not fully working

OpenShift Logging 6.x with LokiStack requires **object storage**. This repo installs the operators via GitOps, but applying the LokiStack CR is optional until storage is configured.
See `gitops/platform/logging/README.md`.

## ODF operator installed but storage classes missing

Installing the **ODF Operator** is not enough to get NooBaa / Ceph StorageClasses.
You must create a `StorageCluster` (environment-specific). See:
- `gitops/platform/odf/README.md`
- `gitops/platform/odf/storagecluster-template.yaml`

