# Run MTA analysis

This guide runs **Migration Toolkit for Applications (MTA)** against the legacy source tree and produces an HTML report you can use live in the demo.

## Prereqs

- MTA CLI installed (`mta-cli`)

## List targets

```bash
mta-cli analyze --list-targets
```

## Run analysis (example)

Use the exact `--target` names returned by `--list-targets` and run the analysis against `legacy/`.

This repo includes a helper script:

```bash
./scripts/mta-analyze-legacy.sh
```

Then open the generated HTML report under `mta-output/`.

Notes:
- Targets vary by MTA version; the demo script will reference the actual target names we use in this repo.

