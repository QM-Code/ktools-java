# API Guide

This page summarizes the public Java API in:

- [`src/ktrace/TraceLogger.java`](../src/ktrace/TraceLogger.java)
- [`src/ktrace/Logger.java`](../src/ktrace/Logger.java)
- [`src/ktrace/OutputOptions.java`](../src/ktrace/OutputOptions.java)
- [`src/ktrace/TraceColors.java`](../src/ktrace/TraceColors.java)

## Core Types

| Type | Purpose |
| --- | --- |
| `TraceLogger` | Library-facing source object for one explicit namespace. |
| `Logger` | Executable-facing registry, selector filter, formatter, and CLI integration surface. |
| `OutputOptions` | Controls file, line, function, and timestamp output formatting. |
| `TraceColors` | Resolves named trace colors and exposes the default color constant. |

## `TraceLogger`

### Construction

```java
TraceLogger trace = new TraceLogger("alpha");
```

Rules:

- the namespace is required
- namespaces use identifier-style tokens such as `alpha`, `core`, or `beta-2`
- invalid namespaces throw `IllegalArgumentException`

### Channel Registration

```java
trace.addChannel("net");
trace.addChannel("net.alpha");
trace.addChannel("cache", TraceColors.color("Gold3"));
```

Rules:

- channels are explicitly registered before use
- nested channels require their parent channel to exist first
- duplicate channel registration is allowed when colors are compatible
- conflicting explicit colors are rejected

### Trace Output

```java
trace.trace("net", "starting request {} for {}", requestId, host);
trace.traceChanged("cache", cacheKey, "cache state changed");
```

`trace(...)`

- emits only when the imported `Logger` has that channel enabled
- validates the channel name before emitting

`traceChanged(...)`

- suppresses repeated emission for the same call site and key value
- emits again when the key changes at that same site

### Operational Logging

```java
trace.info("starting application");
trace.warn("configuration file '{}' was not found", path);
trace.error("fatal startup failure");
```

These methods:

- do not depend on channel enablement
- still include the trace namespace
- still obey current `OutputOptions`

## `Logger`

### Lifecycle

```java
Logger logger = new Logger();
logger.addTraceLogger(appTrace);
logger.addTraceLogger(alphaTrace);
```

The `Logger`:

- imports one or more `TraceLogger` instances
- owns the central registry of registered channels
- rejects attaching the same `TraceLogger` handle to different `Logger` instances

### Exact Channel Control

```java
logger.enableChannel("alpha.net");
logger.enableChannel(appTrace, ".app");

logger.disableChannel("alpha.net");
logger.disableChannel(appTrace, ".app");
```

Exact selectors may use:

- `namespace.channel`
- `.channel` when a local namespace is supplied

If an exact selector names an unregistered channel:

- the call succeeds
- a warning log line is emitted
- the channel remains disabled

### Selector-List Control

```java
logger.enableChannels("*.*");
logger.enableChannels(appTrace, ".app,alpha.net");

logger.disableChannels("alpha.*");
```

These methods:

- accept comma-separated selector expressions
- resolve only against channels already registered with this `Logger`
- ignore unresolved selectors with warning output

### Query Surface

```java
boolean enabled = logger.shouldTraceChannel("alpha.net");
List<String> namespaces = logger.getNamespaces();
List<String> channels = logger.getChannels("alpha");
```

`shouldTraceChannel(...)` returns `false` for:

- invalid selectors
- unregistered channels
- registered but currently disabled channels

### Output Options

```java
logger.setOutputOptions(new OutputOptions(true, true, true, true));
OutputOptions options = logger.getOutputOptions();
```

Fields:

| Field | Meaning |
| --- | --- |
| `filenames` | Include source file labels. |
| `lineNumbers` | Include line numbers when file labels are enabled. |
| `functionNames` | Include method names when file labels are enabled. |
| `timestamps` | Include compact epoch-second timestamps. |

If `filenames` is `false`, line and function labels are suppressed.

### CLI Integration

```java
InlineParser parser = logger.makeInlineParser(appTrace);
InlineParser custom = logger.makeInlineParser(appTrace, "trace");
```

The generated inline parser supports:

- `--trace <selectors>`
- `--trace-examples`
- `--trace-namespaces`
- `--trace-channels`
- `--trace-colors`
- `--trace-files`
- `--trace-functions`
- `--trace-timestamps`

## `TraceColors`

```java
int cyan = TraceColors.color("BrightCyan");
int defaultColor = TraceColors.DEFAULT;
```

Use `TraceColors.color(...)` when you want named colors in demo or application
setup code. Unknown names throw `IllegalArgumentException`.

## Formatting Rules

Message formatting supports:

- sequential `{}` placeholders
- escaped braces `{{` and `}}`

Examples:

```java
trace.trace("app", "value {} {}", 7, "done");
trace.warn("escaped {{}}");
```

Unsupported format tokens such as `{:x}` throw `IllegalArgumentException`.
