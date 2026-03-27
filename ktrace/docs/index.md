# Ktrace Java Documentation

`ktrace` is the Java tracing and logging SDK in the ktools ecosystem.

It is built around two public runtime types:

- `TraceLogger` for library-facing namespaced trace and log emission
- `Logger` for executable-facing channel registration, selector control,
  formatting, and `kcli` integration

## Start Here

- [API guide](api.md)
- [Selector and CLI behavior](selectors.md)
- [Examples](examples.md)

## Typical Flow

```java
import kcli.Parser;
import ktrace.Logger;
import ktrace.TraceColors;
import ktrace.TraceLogger;

Logger logger = new Logger();

TraceLogger appTrace = new TraceLogger("core");
appTrace.addChannel("app", TraceColors.color("BrightCyan"));
appTrace.addChannel("startup", TraceColors.color("BrightYellow"));

logger.addTraceLogger(appTrace);
logger.enableChannel(appTrace, ".app");

Parser parser = new Parser();
parser.addInlineParser(logger.makeInlineParser(appTrace));
parser.parseOrExit(args);

appTrace.trace("app", "startup complete");
```

## Core Concepts

`TraceLogger`

- owns one explicit namespace such as `core`, `alpha`, or `omega`
- registers valid channels for that namespace
- emits channel-based trace output through `trace(...)`
- emits always-visible operational log output through `info/warn/error(...)`

`Logger`

- imports one or more `TraceLogger` instances
- maintains the central registry of known namespaces and channels
- enables and disables channels by exact selector or selector expression
- controls output formatting such as files, functions, and timestamps
- exposes trace CLI controls with `makeInlineParser(...)`

`Selector`

- identifies one or more registered channels using forms such as `.app`,
  `alpha.net`, `*.*`, or `*.{net,io}`

## Build And Explore

`ktrace` depends on the sibling Java `kcli` SDK. If that SDK is not already
built, start from the Java workspace root so both repos build in dependency
order:

```bash
cd ..
kbuild --batch --build-latest
cd ktrace
```

```bash
kbuild --help
kbuild --build-latest
./build/latest/tests/run-tests
./demo/exe/core/build/latest/test --trace '*.*'
./demo/exe/omega/build/latest/test --trace '*.{net,io}'
```

## Working References

- [`src/ktrace/TraceLogger.java`](../src/ktrace/TraceLogger.java)
- [`src/ktrace/Logger.java`](../src/ktrace/Logger.java)
- [`demo/exe/core/src/ktrace/demo/core/Main.java`](../demo/exe/core/src/ktrace/demo/core/Main.java)
- [`demo/exe/omega/src/ktrace/demo/omega/Main.java`](../demo/exe/omega/src/ktrace/demo/omega/Main.java)
- [`tests/src/ktrace/tests/ApiTests.java`](../tests/src/ktrace/tests/ApiTests.java)
