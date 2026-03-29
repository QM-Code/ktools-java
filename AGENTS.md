# ktools-java

Assume `../ktools/AGENTS.md` has already been read.

`ktools-java/` is the Java workspace for the ktools ecosystem.

## What This Level Owns

This workspace owns Java-specific concerns such as:

- Java implementation structure and package layout
- Java build and test flow
- Java-specific API conventions
- coordination across Java tool implementations when more than one repo is present

Cross-language conceptual definitions belong at the overview/spec level, not here.

## Current Scope

This workspace currently contains:

- `kcli/`
- `ktrace/`

A local `kbuild/` directory may also exist for scratch or transitional work, but the shared `kbuild` repo at the ecosystem level remains the canonical implementation.

## Guidance For Agents

1. First determine whether the task belongs at the workspace root or inside a specific implementation repo.
2. Prefer making changes in the narrowest repo that actually owns the behavior.
3. Use the root workspace only for Java-workspace-wide concerns such as root docs or cross-repo coordination.
4. Read the relevant child repo `AGENTS.md` and `README.md` files before changing code in that repo.
5. Use `kbuild` from `PATH` as the intended shared build entrypoint unless the operator explicitly asks you to work on scratch build tooling.

## Git Sync

Use the shared `kbuild` workflow for commit/push sync from this workspace root:

```bash
kbuild --git-sync "<message>"
```

Treat that as the standard sync command unless a more local doc explicitly
overrides it.
After a coherent batch of changes in this workspace or one of its child repos,
return to `ktools-java/` and run that sync command promptly.
