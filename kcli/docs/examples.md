# Examples

This page shows a few common Java `kcli` patterns. For complete working
examples, also see:

- [`demo/sdk/alpha/src/kcli/demo/alpha/AlphaSdk.java`](../demo/sdk/alpha/src/kcli/demo/alpha/AlphaSdk.java)
- [`demo/sdk/beta/src/kcli/demo/beta/BetaSdk.java`](../demo/sdk/beta/src/kcli/demo/beta/BetaSdk.java)
- [`demo/sdk/gamma/src/kcli/demo/gamma/GammaSdk.java`](../demo/sdk/gamma/src/kcli/demo/gamma/GammaSdk.java)
- [`demo/exe/core/src/kcli/demo/core/Main.java`](../demo/exe/core/src/kcli/demo/core/Main.java)
- [`demo/exe/omega/src/kcli/demo/omega/Main.java`](../demo/exe/omega/src/kcli/demo/omega/Main.java)

## Minimal Executable

```java
import kcli.Parser;

Parser parser = new Parser();

parser.addAlias("-v", "--verbose");
parser.setHandler("--verbose", context -> {
}, "Enable verbose logging.");

parser.parseOrExit(args);
```

## Inline Root With Subcommand-Like Options

```java
Parser parser = new Parser();
InlineParser build = new InlineParser("--build");

build.setHandler("-profile", (context, value) -> {
}, "Set build profile.");
build.setHandler("-clean", context -> {
}, "Enable clean build.");

parser.addInlineParser(build);
parser.parseOrExit(args);
```

This enables:

```text
--build
--build-profile release
--build-clean
```

## Bare Root Value Handler

```java
InlineParser config = new InlineParser("--config");

config.setRootValueHandler((context, value) -> {
}, "<assignment>", "Store a config assignment.");
```

This enables:

```text
--config
--config user=alice
```

Behavior:

- `--config` prints inline help
- `--config user=alice` invokes the root value handler

## Alias Preset Tokens

```java
Parser parser = new Parser();

parser.addAlias("-c", "--config", "user-file=/tmp/user.json");
parser.parseOrExit(args);
```

This makes:

```text
-c
```

behave like:

```text
--config user-file=/tmp/user.json
```

Inside the handler:

- `context.option()` is `--config`
- `context.valueTokens()` contains the preset value token

## Optional Values

```java
parser.setOptionalValueHandler("--color", (context, value) -> {
}, "Set or auto-detect color output.");
```

This enables both:

```text
--color
--color always
```

## Positionals

```java
parser.setPositionalHandler(context -> {
    for (String token : context.valueTokens()) {
        usePositional(token);
    }
});
```

The positional handler receives all remaining non-option tokens after option
parsing succeeds.

## Custom Error Handling

If you want your own formatting or exit policy, use `parseOrThrow(...)`:

```java
try {
    parser.parseOrThrow(args);
} catch (CliError ex) {
    System.err.println("custom cli error: " + ex.getMessage());
    System.exit(2);
}
```

Use this when:

- you want custom error text
- you want custom logging
- you want a different exit code policy
