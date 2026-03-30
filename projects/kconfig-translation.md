# Java Kconfig Translation

## Mission

Create a new `ktools-java/kconfig/` component that matches the C++ `kconfig`
behavior while remaining idiomatic for Java.

Use the lessons from `kcli` and `ktrace`: preserve behavior, make public APIs
native to the host language, keep public and internal boundaries obvious, and
do not centralize demo logic into shared demo-support packages.

## Required Reading

- `../ktools/AGENTS.md`
- `AGENTS.md`
- `README.md`
- `kcli/AGENTS.md`
- `kcli/README.md`
- `ktrace/AGENTS.md`
- `ktrace/README.md`
- `../ktools-cpp/kconfig/README.md`
- `../ktools-cpp/kconfig/include/kconfig.hpp`
- `../ktools-cpp/kconfig/include/kconfig/json.hpp`
- `../ktools-cpp/kconfig/include/kconfig/asset.hpp`
- `../ktools-cpp/kconfig/include/kconfig/cli.hpp`
- `../ktools-cpp/kconfig/include/kconfig/store.hpp`
- `../ktools-cpp/kconfig/include/kconfig/store/fs.hpp`
- `../ktools-cpp/kconfig/include/kconfig/store/read.hpp`
- `../ktools-cpp/kconfig/include/kconfig/store/user.hpp`
- `../ktools-cpp/kconfig/cmake/tests/kconfig_json_api_test.cpp`
- `../ktools-cpp/kconfig/demo/bootstrap/README.md`
- `../ktools-cpp/kconfig/demo/sdk/alpha/README.md`
- `../ktools-cpp/kconfig/demo/sdk/beta/README.md`
- `../ktools-cpp/kconfig/demo/sdk/gamma/README.md`
- `../ktools-cpp/kconfig/demo/exe/core/README.md`
- `../ktools-cpp/kconfig/demo/exe/omega/README.md`
- `../ktools-cpp/kconfig/src/kconfig/cli.cpp`
- `../ktools-cpp/kconfig/src/kconfig/store/access.cpp`
- `../ktools-cpp/kconfig/src/kconfig/store/layers.cpp`
- `../ktools-cpp/kconfig/src/kconfig/store/read.cpp`
- `../ktools-cpp/kconfig/src/kconfig/store/bindings.cpp`

## Deliverables

- Add a new `kconfig/` component to the Java workspace.
- Update workspace docs and `.kbuild.json` so the workspace batch order becomes
  `kcli`, `ktrace`, `kconfig`.
- Keep the Java package and module structure explicit and reviewable.
- Provide focused tests and demos, not just a library port.

## Translation Scope

- JSON value model, parse, dump, and typed reads.
- Store registry, mutability, merge, get, set, erase, and typed read helpers.
- Filesystem-backed store helpers, asset roots, and user-config flows.
- `kcli` inline parser integration for config overrides.
- `ktrace` integration for warnings, errors, and operator-facing diagnostics.

## Demo Contract

- The demo tree must be:
  - `demo/bootstrap`
  - `demo/sdk/{alpha,beta,gamma}`
  - `demo/exe/{core,omega}`
- Do not introduce `demo/common` or `demo/sdk/common`.
- Keep each SDK self-contained and keep executable composition inside the
  executable demos.

## Java Rules

- Keep the public API idiomatic for Java.
- Keep package boundaries obvious and do not hide the public surface inside
  internal-only packages.
- Split large files when that materially improves readability, but avoid
  pointless fragmentation.

## Validation

- `cd ktools-java/kconfig && kbuild --build-latest`
- `cd ktools-java/kconfig && kbuild --build-demos`
- Run the component test suite.
- Run the demo commands documented in `ktools-java/kconfig/README.md`.

## Done When

- `ktools-java/kconfig/` exists as a normal Java workspace component.
- The public API is Java-idiomatic and the module layout is easy to review.
- Demo code is explicit and self-contained.
- The workspace root and batch build flow know about `kconfig`.
