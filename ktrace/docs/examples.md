# Examples

This page shows a few common Java `ktrace` patterns. For complete working
examples, also see:

- [`demo/sdk/alpha/src/ktrace/demo/alpha/AlphaSdk.java`](../demo/sdk/alpha/src/ktrace/demo/alpha/AlphaSdk.java)
- [`demo/sdk/beta/src/ktrace/demo/beta/BetaSdk.java`](../demo/sdk/beta/src/ktrace/demo/beta/BetaSdk.java)
- [`demo/sdk/gamma/src/ktrace/demo/gamma/GammaSdk.java`](../demo/sdk/gamma/src/ktrace/demo/gamma/GammaSdk.java)
- [`demo/exe/core/src/ktrace/demo/core/Main.java`](../demo/exe/core/src/ktrace/demo/core/Main.java)
- [`demo/exe/omega/src/ktrace/demo/omega/Main.java`](../demo/exe/omega/src/ktrace/demo/omega/Main.java)

## Minimal Executable

```java
import kcli.Parser;
import ktrace.Logger;
import ktrace.TraceLogger;

Logger logger = new Logger();
TraceLogger trace = new TraceLogger("app");
trace.addChannel("startup");

logger.addTraceLogger(trace);
logger.enableChannel("app.startup");

Parser parser = new Parser();
parser.addInlineParser(logger.makeInlineParser(trace));
parser.parseOrExit(args);

trace.trace("startup", "application started");
```

## Imported SDK Trace Logger

Library code:

```java
public final class AlphaSdk {
    private static final class Holder {
        private static final TraceLogger TRACE_LOGGER = buildTraceLogger();

        private Holder() {
        }

        private static TraceLogger buildTraceLogger() {
            TraceLogger logger = new TraceLogger("alpha");
            logger.addChannel("net");
            logger.addChannel("cache");
            return logger;
        }
    }

    public static TraceLogger getTraceLogger() {
        return Holder.TRACE_LOGGER;
    }
}
```

SDKs should expose a shared `TraceLogger` handle so executables and library code
operate on the same registered channels.

Executable code:

```java
Logger logger = new Logger();
TraceLogger appTrace = new TraceLogger("core");
appTrace.addChannel("app");

logger.addTraceLogger(appTrace);
logger.addTraceLogger(AlphaSdk.getTraceLogger());

logger.enableChannel(appTrace, ".app");
logger.enableChannels("*.*");
```

## Local Namespace Selectors

```java
TraceLogger appTrace = new TraceLogger("omega");
appTrace.addChannel("app");
appTrace.addChannel("deep");
appTrace.addChannel("deep.branch");

logger.addTraceLogger(appTrace);

logger.enableChannel(appTrace, ".app");
logger.enableChannels(appTrace, ".app,.deep.branch");
```

Using the local-trace overload lets leading-dot selectors resolve against the
correct namespace.

## Output Formatting

```java
logger.setOutputOptions(new OutputOptions(true, true, true, true));

trace.trace("app", "formatted trace line");
trace.warn("formatted warning");
```

This enables:

- source file labels
- line numbers
- method names
- timestamps

## `traceChanged(...)`

```java
trace.addChannel("state");
logger.addTraceLogger(trace);
logger.enableChannel("app.state");

trace.traceChanged("state", currentState, "state changed to {}", currentState);
```

This is useful when:

- the same polling or update site runs frequently
- you only want a log line when the observed key changes

## Trace CLI

```java
Parser parser = new Parser();
parser.addInlineParser(logger.makeInlineParser(appTrace));
parser.parseOrExit(args);
```

Examples:

```text
--trace '.app'
--trace '*.*'
--trace '*.{net,io}'
--trace-files
--trace-functions
--trace-timestamps
--trace-channels
```
