# Java ktrace Project

## Mission

Make `ktools-java/ktrace/` a tidy, explicit peer to the C++ reference while
keeping the current strong Java package structure.

## Required Reading

- `../ktools/AGENTS.md`
- `AGENTS.md`
- `ktrace/AGENTS.md`
- `ktrace/README.md`
- `ktrace/docs/api.md`
- `ktrace/docs/selectors.md`
- `../ktools-cpp/ktrace/README.md`
- `../ktools-cpp/ktrace/include/ktrace.hpp`
- `../ktools-cpp/ktrace/src/ktrace/cli.cpp`
- `../ktools-cpp/ktrace/cmake/tests/ktrace_channel_semantics_test.cpp`
- `../ktools-cpp/ktrace/cmake/tests/ktrace_format_api_test.cpp`
- `../ktools-cpp/ktrace/cmake/tests/ktrace_log_api_test.cpp`

## Current Gaps

- `ktrace/demo/sdk/common/` exists and should not.
- The Java repo has no dedicated `demo/tests/` layer, so demo behavior still
  relies heavily on manual review.
- The implementation needs a careful parity audit against the C++ contract for
  selectors, logging behavior, and CLI integration.
- Docs and demos should be checked so they explain behavior directly instead
  of relying on reader inference.

## Work Plan

1. Eliminate shared demo code.
- Remove `ktrace/demo/sdk/common/`.
- Make `demo/sdk/alpha`, `demo/sdk/beta`, and `demo/sdk/gamma` self-contained.
- Keep bootstrap-specific logic under `demo/bootstrap/`.
- Keep executable composition logic under `demo/exe/core/` and
  `demo/exe/omega/`.
- Do not create a replacement shared demo support package.

2. Audit parity with C++ in detail.
- Compare Java behavior against the C++ contract for channel registration,
  duplicate color merges, exact selectors, selector lists, unmatched selector
  warnings, output options, `traceChanged(...)`, and logger-bound
  `makeInlineParser(...)`.
- Add focused tests for any reference behavior that is not asserted directly.

3. Treat demos as contract checks.
- Review `demo/bootstrap`, `demo/sdk/{alpha,beta,gamma}`, and
  `demo/exe/{core,omega}` as first-class contract material.
- Add demo-focused coverage if the current validation still depends too much on
  manual inspection.

4. Tighten documentation where needed.
- Update local docs if the Java repo still requires inference about behavior or
  layout.
- Avoid adding Java-only semantics unless there is a strong reason.

5. Keep hygiene rules explicit.
- Keep generated output out of version control.
- Do not let future cleanup regress.

## Constraints

- Preserve the current strong package split.
- Avoid cosmetic refactors that do not improve clarity or parity.
- Keep the public API conceptually aligned with C++.

## Validation

- `cd ktools-java/ktrace && kbuild --build-latest`
- `cd ktools-java/ktrace && kbuild --build-demos`
- `cd ktools-java/ktrace && ./build/latest/tests/run-tests`
- Run the demo launchers listed in `ktools-java/ktrace/README.md`

## Done When

- Shared demo code is gone.
- Tests, demos, and docs cover the C++ contract directly.
- The Java repo is easy to compare with the reference.
