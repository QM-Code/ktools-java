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

```bash
python3 ../kbuild/kbuild.py --build-latest
```

SDK output:

- `build/latest/sdk/classes`

## Build And Test

```bash
python3 ../kbuild/kbuild.py --build-latest
./build/latest/tests/run-tests
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
