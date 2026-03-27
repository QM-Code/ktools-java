# Karma Trace Logging SDK

`ktrace` is the Java tracing and logging SDK in the ktools ecosystem.

It provides:

- namespaced channel tracing via `TraceLogger.trace(...)`
- always-visible operational logging via `TraceLogger.info/warn/error(...)`
- executable-facing channel filtering and formatting via `Logger`
- `kcli` inline parser integration through `Logger.makeInlineParser(...)`

## Documentation

- [Overview](docs/index.md)
- [API guide](docs/api.md)
- [Selector and CLI behavior](docs/selectors.md)
- [Examples](docs/examples.md)

## Build SDK

`ktrace` depends on the sibling Java `kcli` SDK. Build `ktools-java/kcli` first,
or use the Java workspace root to build both in dependency order:

```bash
cd ..
kbuild --batch --build-latest
cd ktrace
```

```bash
kbuild --build-latest
```

SDK output:

- `build/latest/sdk/classes`

## Build And Test Demos

If `kcli` has not been built yet in this workspace, prefer the batch build from
`ktools-java/` first so the dependency SDK is present.

```bash
# Builds the SDK plus demos listed in .kbuild.json build.defaults.demos.
kbuild --build-latest

# Explicit demo-only run (uses .kbuild.json build.demos when no args are passed).
kbuild --build-demos

./build/latest/tests/run-tests
```

Demos:

- Bootstrap compile/link check: `demo/bootstrap/`
- SDKs: `demo/sdk/{alpha,beta,gamma}`
- Executables: `demo/exe/{core,omega}`

Trace CLI examples:

```bash
./demo/exe/core/build/latest/test --trace
./demo/exe/core/build/latest/test --trace '.*'
./demo/exe/omega/build/latest/test --trace '*.*'
./demo/exe/omega/build/latest/test --trace '*.*.*.*'
./demo/exe/omega/build/latest/test --trace '*.{net,io}'
./demo/exe/omega/build/latest/test --trace-namespaces
./demo/exe/omega/build/latest/test --trace-channels
./demo/exe/omega/build/latest/test --trace-colors
```

## API Model

```java
import ktrace.Logger;
import ktrace.TraceColors;
import ktrace.TraceLogger;

Logger logger = new Logger();

TraceLogger appTrace = new TraceLogger("core");
appTrace.addChannel("app", TraceColors.color("BrightCyan"));
appTrace.addChannel("startup", TraceColors.color("BrightYellow"));

logger.addTraceLogger(appTrace);
logger.enableChannel(appTrace, ".app");
appTrace.trace("app", "core initialized");
```

## CLI Integration

```java
kcli.Parser parser = new kcli.Parser();
parser.addInlineParser(logger.makeInlineParser(appTrace));
parser.parseOrExit(argv.length, argv);
```
