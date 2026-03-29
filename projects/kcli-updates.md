# Java kcli Project

## Mission

Bring `ktools-java/kcli/` fully up to the C++ reference standard. The source
architecture is already strong, so this project is mostly about repo hygiene,
small structural cleanup, and behavior completeness.

## Required Reading

- `../ktools/AGENTS.md`
- `AGENTS.md`
- `kcli/AGENTS.md`
- `kcli/README.md`
- `../ktools-cpp/kcli/README.md`
- `../ktools-cpp/kcli/docs/behavior.md`
- `../ktools-cpp/kcli/cmake/tests/kcli_api_cases.cpp`

## Current Gaps

- The Java source/test/demo layout is one of the best non-C++ layouts.
- The main structural problem is tracked generated output:
  `kcli/src/**/*.class`, `kcli/build/latest/**`, and `kcli/demo/**/build/latest/**`.
- `kcli/src/kcli/internal/Model.java` still bundles many internal types into one
  file.
- The implementation should be audited for any remaining behavior drift from
  the C++ docs and tests.

## Work Plan

1. Clean the repo.
- Remove tracked `.class` files from `src/`.
- Remove tracked build output from `build/latest` and demo build trees.
- Add or tighten ignore rules so generated output does not return.

2. Keep the good source structure, but finish the cleanup.
- Preserve the current split between `src/kcli/` and `src/kcli/internal/`.
- Consider splitting `Model.java` if that materially improves readability.
- Do not refactor away the current strengths of the repo.

3. Confirm behavior parity with C++.
- Compare Java behavior against the C++ docs and test contract, especially for
  aliases, inline roots, bare-root help, option normalization, optional values,
  required values, and error handling.
- Add focused tests for any reference behavior that is not currently asserted.

4. Keep demos as contract checks.
- Ensure `demo/bootstrap`, `demo/sdk/{alpha,beta,gamma}`, and
  `demo/exe/{core,omega}` still match the reference roles.
- Keep demo source readable enough that other agents can learn from it.

5. Tighten documentation where needed.
- Update local docs if the Java implementation currently requires inference.
- Avoid adding Java-only semantics unless there is a very strong reason.

## Constraints

- Preserve the current strong package split.
- Avoid cosmetic refactors that do not improve navigability or parity.
- Keep the public API conceptually aligned with C++.

## Validation

- `cd ktools-java/kcli && kbuild --build-latest`
- `cd ktools-java/kcli && kbuild --build-demos`
- `cd ktools-java/kcli && ./build/latest/tests/run-tests`
- Run the demo launchers listed in `ktools-java/kcli/README.md`

## Done When

- The repo no longer mixes hand-written sources with generated class files.
- The Java implementation remains one of the closest mirrors of the C++
  structure.
- Docs, tests, and demos together make Java a clean second reference.
