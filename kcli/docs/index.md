# Kcli Java Documentation

`kcli` is the Java command-line parsing SDK in the ktools ecosystem.

It is intentionally opinionated about normal CLI behavior:

- parse first
- fail early on invalid input
- do not run handlers until the full command line validates
- preserve the caller's argument array
- support grouped inline roots such as `--trace-*` and `--config-*`

## Start Here

- [API guide](api.md)
- [Parsing behavior](behavior.md)
- [Examples](examples.md)

## Typical Flow

```java
import kcli.InlineParser;
import kcli.Parser;

Parser parser = new Parser();
InlineParser build = new InlineParser("--build");

build.setHandler("-profile", (context, value) -> {
}, "Set build profile.");

parser.addInlineParser(build);
parser.addAlias("-v", "--verbose");
parser.setHandler("--verbose", context -> {
}, "Enable verbose logging.");

parser.parseOrExit(args);
```

## Core Concepts

`Parser`

- owns top-level handlers, aliases, positional handling, and inline parser registrations

`InlineParser`

- defines one inline root namespace such as `--alpha`, `--trace`, or `--build`

`HandlerContext`

- exposes the effective option, command, root, and value tokens seen by the
  handler after alias expansion

`CliError`

- used by `parseOrThrow(...)` to surface invalid CLI input and handler failures

## Which Entry Point Should I Use?

Use `parseOrExit(...)` when:

- you are in a normal executable `main(...)`
- invalid CLI input should print a standardized error and exit with code `2`
- you do not need custom formatting or recovery

Use `parseOrThrow(...)` when:

- you want to customize error formatting
- you want custom exit codes
- you want to intercept and test parse failures directly

## Build And Explore

```bash
kbuild --help
kbuild --build-latest
./demo/exe/core/build/latest/test --alpha-message hello
./demo/exe/omega/build/latest/test --build
```

## Working References

If you want complete, working examples, start with:

- [`demo/sdk/alpha/src/kcli/demo/alpha/AlphaSdk.java`](../demo/sdk/alpha/src/kcli/demo/alpha/AlphaSdk.java)
- [`demo/exe/core/src/kcli/demo/core/Main.java`](../demo/exe/core/src/kcli/demo/core/Main.java)
- [`demo/exe/omega/src/kcli/demo/omega/Main.java`](../demo/exe/omega/src/kcli/demo/omega/Main.java)
- [`tests/src/kcli/tests/ApiTests.java`](../tests/src/kcli/tests/ApiTests.java)

The public API contract lives in [`src/kcli/`](../src/kcli/).
