# Java Updates

## Mission

Keep `ktools-java/` as a tidy, explicit peer to the C++ reference across both
`kcli` and `ktrace` while preserving the current strong package structure.

## Required Reading

- `../ktools/AGENTS.md`
- `AGENTS.md`
- `README.md`
- `kcli/AGENTS.md`
- `kcli/README.md`
- `ktrace/AGENTS.md`
- `ktrace/README.md`
- `ktrace/docs/api.md`
- `ktrace/docs/selectors.md`
- `../ktools-cpp/kcli/README.md`
- `../ktools-cpp/kcli/docs/behavior.md`
- `../ktools-cpp/kcli/cmake/tests/kcli_api_cases.cpp`
- `../ktools-cpp/ktrace/README.md`
- `../ktools-cpp/ktrace/include/ktrace.hpp`
- `../ktools-cpp/ktrace/src/ktrace/cli.cpp`
- `../ktools-cpp/ktrace/cmake/tests/ktrace_channel_semantics_test.cpp`
- `../ktools-cpp/ktrace/cmake/tests/ktrace_format_api_test.cpp`
- `../ktools-cpp/ktrace/cmake/tests/ktrace_log_api_test.cpp`

## kcli Focus

- Remove the leftover `kcli/demo/sdk/common/` directories. There should not be
  a shared demo layer here.
- Review whether splitting `kcli/src/kcli/internal/Model.java` would
  materially improve navigability.
- Re-audit full parser parity with C++ for aliases, inline roots, bare-root
  help, option normalization, optional and required values, and error
  behavior.

## ktrace Focus

- Remove the leftover `ktrace/demo/sdk/common/` directories. There should not
  be a shared demo layer here either.
- Add explicit bootstrap demo coverage so the demo-contract tests are not
  skewed toward core and omega only.
- Review whether splitting `src/ktrace/internal/TraceInternals.java` would
  improve navigability without creating file sprawl.
- Re-audit selector, logger, output, and CLI behavior against the C++ contract.

## Cross-Cutting Rules

- Preserve the current package split unless there is a strong readability case
  for change.
- Keep demos readable as separate entities: `bootstrap`,
  `sdk/{alpha,beta,gamma}`, and `exe/{core,omega}`.
- Keep generated output out of the handwritten source tree.

## Validation

- `cd ktools-java/kcli && kbuild --build-latest`
- `cd ktools-java/kcli && kbuild --build-demos`
- `cd ktools-java/kcli && ./build/latest/tests/run-tests`
- `cd ktools-java/ktrace && kbuild --build-latest`
- `cd ktools-java/ktrace && kbuild --build-demos`
- `cd ktools-java/ktrace && ./build/latest/tests/run-tests`
- Run the demo launchers listed in each repo README

## Done When

- The leftover demo-common directories are gone.
- Java `kcli` and `ktrace` are both explicit, parity-checked peers to C++.
- Docs, tests, and demos make the current structure easy to follow.
