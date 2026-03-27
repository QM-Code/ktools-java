# ktools-java

`ktools-java/` is the Java workspace for the broader ktools ecosystem.

It is the root entrypoint for Java implementations of the ktools libraries.

## Current Contents

This workspace currently contains:

- `kcli/`
- `ktrace/`

A local `kbuild/` directory may also be present for scratch or transitional work, but the intended shared build entrypoint is `kbuild` on `PATH`.

## Build Model

Use the relevant child repo when building or testing a specific Java implementation.

Typical shared build expectations:

```bash
kbuild --help
kbuild --batch --build-latest
```

When additional Java tool repos are added to this workspace, this root becomes the natural entrypoint for Java-stack-wide documentation and coordination.

## Where To Go Next

For concrete Java API and implementation details, use the docs in the relevant child repo.

Current implementation:

- [kcli](kcli)
- [ktrace](ktrace)
