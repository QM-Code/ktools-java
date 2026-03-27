# Selector And CLI Behavior

This page summarizes selector matching and the generated `--trace-*` CLI
behavior.

## Exact Selectors

Exact channel APIs:

- `enableChannel(...)`
- `disableChannel(...)`
- `shouldTraceChannel(...)`

Accepted forms:

- `namespace.channel`
- `namespace.channel.sub`
- `.channel`
- `.channel.sub`

Rules:

- `.channel` requires a local namespace, such as `enableChannel(appTrace, ".app")`
- exact selectors must point to registered channels to become enabled
- invalid exact selectors throw `IllegalArgumentException` in mutating APIs
- `shouldTraceChannel(...)` returns `false` instead of throwing

## Selector Lists

Selector-list APIs:

- `enableChannels(...)`
- `disableChannels(...)`

Accepted forms include:

- `*.*`
- `*.*.*`
- `*.*.*.*`
- `alpha.*`
- `alpha.*.*`
- `*.net`
- `*.scheduler.tick`
- `*.{net,io}`
- `{alpha,beta}.*`
- `.app,alpha.net`

Rules:

- input is comma-separated
- brace expansion is supported
- matching is performed against currently registered channels only
- unmatched selectors are reported as warnings
- empty or whitespace-only selector lists are rejected

## Depth Semantics

Channel depth is limited to three segments:

- `top`
- `top.child`
- `top.child.leaf`

Wildcard meaning:

| Selector | Meaning |
| --- | --- |
| `*.*` | All top-level namespace channels. |
| `*.*.*` | All channels up to depth 2. |
| `*.*.*.*` | All channels up to depth 3. |

`*.{net,io}` matches only those exact top-level channel names across all
namespaces.

## Generated Trace CLI

`Logger.makeInlineParser(localTrace)` creates an inline root that can be
attached to `kcli.Parser`:

```java
Parser parser = new Parser();
parser.addInlineParser(logger.makeInlineParser(appTrace));
```

This enables:

```text
--trace
--trace '*.*'
--trace '.app'
--trace-examples
--trace-namespaces
--trace-channels
--trace-colors
--trace-files
--trace-functions
--trace-timestamps
```

### Bare Root

```text
--trace
```

Behavior:

- prints inline help
- does not enable any channels

### Root Value

```text
--trace '.app'
--trace '*.{net,io}'
```

Behavior:

- resolves the selector expression
- enables matching registered channels
- warns for unmatched selectors

### Formatting Options

These toggle `Logger` output formatting:

- `--trace-files`
- `--trace-functions`
- `--trace-timestamps`

Effects:

- `--trace-files` enables file and line labels
- `--trace-functions` enables file, line, and method labels
- `--trace-timestamps` enables timestamps for both trace and operational logs

## Failure Model

Invalid `--trace <selector>` input is surfaced through `kcli` as a normal
option failure. For example:

```text
[error] [cli] option '--trace': Invalid trace selector: '*' (did you mean '.*'?)
```

Unknown generated options such as `--trace-lines` are treated as ordinary
unknown `kcli` options.
