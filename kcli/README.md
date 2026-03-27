# Karma CLI Parsing SDK

`kcli` is the Java command-line parsing SDK in the ktools ecosystem.

It is designed around two common CLI shapes:

- top-level options such as `--verbose` and `--output`
- inline roots such as `--trace-*`, `--config-*`, and `--build-*`

The library gives you two explicit entrypoints:

- `parseOrExit(...)` for normal executable startup
- `parseOrThrow(...)` when the caller wants to intercept `CliError`

## Documentation

- [Overview](docs/index.md)
- [API guide](docs/api.md)
- [Parsing behavior](docs/behavior.md)
- [Examples](docs/examples.md)

## Quick Start

```java
import kcli.InlineParser;
import kcli.Parser;

public final class Main {
    private Main() {
    }

    public static void main(String[] args) {
        Parser parser = new Parser();
        InlineParser build = new InlineParser("--build");

        build.setHandler("-profile", (context, value) -> {
        }, "Set build profile.");

        parser.addInlineParser(build);
        parser.addAlias("-v", "--verbose");
        parser.setHandler("--verbose", context -> {
        }, "Enable verbose logging.");

        parser.parseOrExit(args);
    }
}
```

## Behavior Highlights

- The full command line is validated before any registered handler runs.
- `parseOrExit(...)` reports invalid CLI input to `stderr` and exits with code `2`.
- `parseOrThrow(...)` throws `CliError`.
- Bare inline roots such as `--build` print inline help unless a root value is provided.
- `setHandler(..., ValueHandler, ...)` registers a required-value option.
- `setOptionalValueHandler(...)` registers an optional-value option.
- Required values may consume a first token that begins with `-`.
- Literal `--` is rejected as an unknown option; it is not treated as an option terminator.

For the full parsing rules, see [docs/behavior.md](docs/behavior.md).

## Build SDK

```bash
kbuild --build-latest
```

SDK output:

- `build/latest/sdk/classes`

## Build And Test Demos

```bash
# Builds the SDK plus demos listed in .kbuild.json build.defaults.demos.
kbuild --build-latest

# Explicit demo-only run (uses .kbuild.json build.demos when no args are passed).
kbuild --build-demos
```

Demo directories:

- Bootstrap compile/link check: `demo/bootstrap/`
- SDK demos: `demo/sdk/{alpha,beta,gamma}`
- Executable demos: `demo/exe/{core,omega}`

Useful demo commands:

```bash
./demo/exe/core/build/latest/test
./demo/exe/core/build/latest/test --alpha
./demo/exe/core/build/latest/test --alpha-message hello
./demo/exe/core/build/latest/test --output stdout
./demo/exe/omega/build/latest/test --beta-workers 8
./demo/exe/omega/build/latest/test --newgamma-tag prod
./demo/exe/omega/build/latest/test --build
```

## Repository Layout

- Public API: `src/kcli/`
- Library implementation details: `src/kcli/internal/`
- API and CLI coverage: `tests/src/kcli/tests/`
- Integration demos: `demo/`
