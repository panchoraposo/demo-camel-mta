# OpenShift Logging (ODF-backed)

This repo configures OpenShift Logging 6.x with LokiStack backed by **OpenShift Data Foundation (ODF)** object storage (NooBaa S3).

It provisions an ODF bucket via `ObjectBucketClaim` and then creates the Loki secret (`logging-loki-s3`) automatically using a Job running `oc`.

## How to enable

1) Ensure ODF (NooBaa) is installed and the storage classes exist:
- `openshift-storage.noobaa.io` (object buckets)
- `openshift-storage.ceph.rbd` (block/PVC storage)

2) Sync `gitops/platform/` via Argo CD.

## References

- LokiStack object storage secret keys and ODF/NooBaa notes:
  `https://docs.redhat.com/en/documentation/red_hat_openshift_logging/6.5/html/installing_logging/configuring-storage-for-lokistack`

