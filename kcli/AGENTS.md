# kcli-java

Assume these have already been read:

- `../../ktools/AGENTS.md`
- `../AGENTS.md`

`ktools-java/kcli/` is the Java implementation of `kcli`.

## What This Component Owns

This component owns the Java API and implementation details for `kcli`, including:

- public Java parsing APIs
- parser and inline-parser behavior
- `kbuild` packaging for the Java SDK
- Java demos and tests

Cross-language conceptual behavior belongs to the `ktools/` overview docs. Java
workspace concerns belong to `ktools-java/`.

## Local Bootstrap

When familiarizing yourself with this component, read:

- [README.md](README.md)
- `src/*`
- `tests/*`
- `demo/*`

## Build And Test Expectations

- Use `kbuild` from the component root.
- Prefer end-to-end checks using the demo launchers under `demo/*/build/<slot>/`.
- Keep tests explicit and focused on parsing/runtime behavior.

Useful commands:

```bash
kbuild --help
kbuild --build-latest
./build/latest/tests/run-tests
```

After a coherent batch of changes in `ktools-java/kcli/`, return to the
`ktools-java/` workspace root and run `kbuild --git-sync "<message>"`.
