# Java kcli Project

## Mission

Make `ktools-java/kcli/` an explicit, tidy second reference to the C++ design
without losing the current strong package structure.

## Required Reading

- `../ktools/AGENTS.md`
- `AGENTS.md`
- `kcli/AGENTS.md`
- `kcli/README.md`
- `../ktools-cpp/kcli/README.md`
- `../ktools-cpp/kcli/docs/behavior.md`
- `../ktools-cpp/kcli/cmake/tests/kcli_api_cases.cpp`

## Current Gaps

- `kcli/src/kcli/internal/Model.java` still bundles many internal types into
  one file.
- The implementation needs a careful parity audit against the full C++ contract
  even though the current layout is strong.
- Docs and demos should be checked to ensure they explain behavior directly
  instead of relying on reader inference.
- Generated output cleanup is mostly in place, but the repo should keep a tight
  policy so `.class` files and build trees do not drift back into version
  control.

## Work Plan

1. Revisit the internal model layout.
- Review whether splitting `Model.java` would materially improve readability
  and discoverability.
- Preserve the current `src/kcli/` and `src/kcli/internal/` package split.

2. Audit parity with C++ in detail.
- Compare Java behavior against the C++ docs and case list for aliases, inline
  roots, bare-root help, option normalization, optional values, required
  values, and error handling.
- Add focused tests for any reference behavior that is not asserted directly.

3. Treat demos as contract checks.
- Confirm that `demo/bootstrap`, `demo/sdk/{alpha,beta,gamma}`, and
  `demo/exe/{core,omega}` still match the intended reference roles.
- Keep demo code readable enough that other agents can study it.

4. Tighten documentation where needed.
- Update local docs if the Java repo still requires inference about behavior or
  layout.
- Avoid adding Java-only semantics unless there is a strong reason.

5. Keep hygiene rules explicit.
- Make sure ignore rules and repo policy continue to keep generated output out
  of version control.
- Do not let future cleanup regress.

## Constraints

- Preserve the current strong package split.
- Avoid cosmetic refactors that do not improve clarity or parity.
- Keep the public API conceptually aligned with C++.

## Validation

- `cd ktools-java/kcli && kbuild --build-latest`
- `cd ktools-java/kcli && kbuild --build-demos`
- `cd ktools-java/kcli && ./build/latest/tests/run-tests`
- Run the demo launchers listed in `ktools-java/kcli/README.md`

## Done When

- Internal model/types are easy to navigate.
- Tests, demos, and docs cover the C++ contract directly.
- Generated output stays out of the hand-written source tree.
