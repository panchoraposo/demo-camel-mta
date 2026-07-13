# OpenShift Data Foundation (ODF) StorageCluster (optional overlay)

This repo installs the **ODF Operator** via GitOps (`gitops/operators/odf/`).

Creating a **StorageCluster** is environment-specific (nodes, devices, storage class, encryption, etc.), so it is provided as an optional overlay/template.

## What this demo needs from ODF

- NooBaa (MCG) so that `openshift-storage.noobaa.io` exists and `ObjectBucketClaim` provisioning works.
- Ceph RBD so that `openshift-storage.ceph.rbd` exists for LokiStack PVCs.

## How to enable (high level)

1) Prepare worker nodes / disks according to your platform.
2) Apply a StorageCluster manifest derived from `storagecluster-template.yaml`.
3) Wait until ODF creates the required StorageClasses.

## References

- ODF 4.16 bare metal deployment guide:
  `https://docs.redhat.com/en/documentation/red_hat_openshift_data_foundation/4.16/html-single/deploying_openshift_data_foundation_using_bare_metal_infrastructure/index`

