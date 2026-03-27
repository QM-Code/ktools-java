# ktrace-java

Assume these have already been read:

- `../../ktools/AGENTS.md`
- `../../ktrace/AGENTS.md`
- `../AGENTS.md`

`ktools-java/ktrace/` is the Java implementation of `ktrace`.

## What This Repo Owns

This repo owns the Java API and implementation details for `ktrace`, including:

- public Java tracing/logging APIs
- selector parsing and logger runtime behavior
- `kcli` inline parser integration for trace controls
- Java demos and tests

Cross-language conceptual behavior belongs to the `ktrace/` overview repo. Java
workspace concerns belong to `ktools-java/`.

## Local Bootstrap

When familiarizing yourself with this repo, read:

- [README.md](README.md)
- `src/*`
- `tests/*`
- `demo/*`

## Build And Test Expectations

- Use the local `../kbuild/kbuild.py` entrypoint from the repo root.
- Prefer end-to-end checks using the demo launchers under `demo/*/build/<slot>/`.
- Keep tests explicit and focused on trace/runtime behavior.

Useful commands:

```bash
python3 ../kbuild/kbuild.py --help
python3 ../kbuild/kbuild.py --build-latest
./build/latest/tests/run-tests
```
