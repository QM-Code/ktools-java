# API Guide

This page summarizes the public Java API in [`src/kcli/`](../src/kcli/).

## Core Types

| Type | Purpose |
| --- | --- |
| `Parser` | Owns aliases, top-level handlers, positional handling, and inline parser registration. |
| `InlineParser` | Defines one inline root namespace such as `--build` plus its `--build-*` handlers. |
| `HandlerContext` | Metadata delivered to flag, value, and positional handlers. |
| `CliError` | Exception used by `parseOrThrow(...)` for invalid CLI input and handler failures. |

## HandlerContext

`HandlerContext` is passed to every handler.

| Field | Meaning |
| --- | --- |
| `root()` | Inline root name without leading dashes, such as `build`. Empty for top-level handlers and positional dispatch. |
| `option()` | Effective option token after alias expansion, such as `--verbose` or `--build-profile`. Empty for positional dispatch. |
| `command()` | Normalized command name without leading dashes. Empty for positional dispatch and inline root value handlers. |
| `valueTokens()` | Effective value tokens after alias expansion. Tokens from the shell are preserved verbatim; alias preset tokens are prepended. |

## CliError

`parseOrThrow(...)` throws `CliError` when:

- the command line is invalid
- a registered option handler throws
- the positional handler throws

`option()` returns the option token associated with the failure when one exists.
For positional-handler failures and parser-global errors, it may be empty.

## InlineParser

### Construction

```java
InlineParser parser = new InlineParser("--build");
```

The root may be provided as either:

- `"build"`
- `"--build"`

### Root Value Handler

```java
parser.setRootValueHandler(handler);
parser.setRootValueHandler(handler, "<selector>", "Select build targets.");
```

The root value handler processes the bare root form, for example:

- `--build release`
- `--config user.json`

If the bare root is used without a value, `kcli` prints inline help for that
root instead.

### Inline Handlers

```java
parser.setHandler("-flag", handler, "Enable build flag.");
parser.setHandler("-profile", handler, "Set build profile.");
parser.setOptionalValueHandler("-enable", handler, "Enable build mode.");
```

Inline handler options may be written in either form:

- short inline form: `-profile`
- fully-qualified form: `--build-profile`

## Parser

### Top-Level Handlers

```java
parser.setHandler("--verbose", context -> {
}, "Enable verbose logging.");
parser.setHandler("--output", (context, value) -> {
}, "Set output target.");
parser.setOptionalValueHandler("--color", (context, value) -> {
}, "Set or auto-detect color output.");
```

Top-level handler options may be written as either:

- `"verbose"`
- `"--verbose"`

### Aliases

```java
parser.addAlias("-v", "--verbose");
parser.addAlias("-c", "--config", "user-file=/tmp/user.json");
```

Rules:

- aliases use single-dash form such as `-v`
- alias targets use double-dash form such as `--verbose`
- preset tokens are prepended to the handler's effective `valueTokens()`

### Positional Handler

```java
parser.setPositionalHandler(context -> {
});
```

The positional handler receives remaining non-option tokens in
`HandlerContext.valueTokens()`.

### Inline Parser Registration

```java
parser.addInlineParser(buildParser);
```

Duplicate inline roots are rejected.

### Parse Entry Points

```java
parser.parseOrExit(argc, argv);
parser.parseOrThrow(argc, argv);

parser.parseOrExit(args);
parser.parseOrThrow(args);
```

`parseOrExit(...)`

- preserves the caller's argument array
- reports invalid CLI input to `stderr` as `[error] [cli] ...`
- exits with code `2`

`parseOrThrow(...)`

- preserves the caller's argument array
- throws `CliError`
- does not run handlers until the full command line validates

## Value Handler Registration

Use the registration form that matches the CLI contract you want:

- `setHandler(option, FlagHandler, description)` for flag-style options
- `setHandler(option, ValueHandler, description)` for required values
- `setOptionalValueHandler(option, ValueHandler, description)` for optional values
- `setRootValueHandler(...)` for bare inline roots such as `--build release`

## API Notes

- `Parser` and `InlineParser` are mutable builder-style objects.
- `InlineParser.copy()` creates a detached copy that can be retargeted with `setRoot(...)`.
- The public `src/kcli/` package is intended to be the source-of-truth contract
  for library consumers.
