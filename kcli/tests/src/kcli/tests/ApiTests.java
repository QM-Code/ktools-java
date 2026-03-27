package kcli.tests;

import java.util.ArrayList;
import java.util.List;

import kcli.CliError;
import kcli.HandlerContext;
import kcli.InlineParser;
import kcli.Parser;

final class ApiTests {
    private ApiTests() {
    }

    static void run() throws Exception {
        testParserEmptyParseSucceeds();
        testUnknownOptionDoesNotRunHandlers();
        testAliasRewritesOption();
        testAliasPresetTokensSatisfyRequiredValue();
        testAliasPresetTokensApplyToInlineRootValue();
        testAliasPresetTokensRejectedForFlags();
        testRequiredValueAcceptsOptionLikeFirstToken();
        testBareInlineRootPrintsHelp();
        testDoubleDashRemainsUnknown();
    }

    private static void testParserEmptyParseSucceeds() {
        String[] argv = {"prog"};
        Parser parser = new Parser();
        parser.parseOrThrow(argv.length, argv);
        Assertions.expectEquals(List.of(argv), List.of("prog"), "parseOrThrow should leave argv unchanged");
    }

    private static void testUnknownOptionDoesNotRunHandlers() {
        String[] argv = {"prog", "--verbose", "pos1", "--output", "stdout", "--bogus"};
        final boolean[] verbose = {false};
        final String[] output = {""};
        final List<String> positionals = new ArrayList<>();

        Parser parser = new Parser();
        parser.setHandler("--verbose", context -> verbose[0] = true, "Enable verbose logging.");
        parser.setHandler("--output", (context, value) -> output[0] = value, "Set output target.");
        parser.setPositionalHandler(context -> positionals.addAll(context.valueTokens()));

        CliError error = Assertions.expectThrows(
            CliError.class,
            () -> parser.parseOrThrow(argv.length, argv),
            "unknown option should fail before handlers run");

        Assertions.expect(!verbose[0], "verbose handler should not have run");
        Assertions.expectEquals(output[0], "", "value handler should not have run");
        Assertions.expectEquals(positionals, List.of(), "positional handler should not have run");
        Assertions.expectEquals(error.option(), "--bogus", "CliError option should match unknown token");
        Assertions.expectContains(error.getMessage(), "unknown option --bogus", "CliError should describe unknown option");
    }

    private static void testAliasRewritesOption() {
        String[] argv = {"prog", "-v"};
        final String[] seen = {""};

        Parser parser = new Parser();
        parser.addAlias("-v", "--verbose");
        parser.setHandler("--verbose", context -> seen[0] = context.option(), "Enable verbose logging.");
        parser.parseOrThrow(argv.length, argv);

        Assertions.expectEquals(seen[0], "--verbose", "alias should rewrite the effective option");
    }

    private static void testAliasPresetTokensSatisfyRequiredValue() {
        String[] argv = {"prog", "-p"};
        final String[] value = {""};
        final List<String> tokens = new ArrayList<>();

        Parser parser = new Parser();
        parser.addAlias("-p", "--profile", "release");
        parser.setHandler("--profile", (context, captured) -> {
            value[0] = captured;
            tokens.addAll(context.valueTokens());
        }, "Set active profile.");
        parser.parseOrThrow(argv.length, argv);

        Assertions.expectEquals(value[0], "release", "preset token should satisfy required value");
        Assertions.expectEquals(tokens, List.of("release"), "context tokens should include preset value");
    }

    private static void testAliasPresetTokensApplyToInlineRootValue() {
        String[] argv = {"prog", "-c"};
        final String[] option = {""};
        final String[] value = {""};

        Parser parser = new Parser();
        InlineParser config = new InlineParser("--config");
        config.setRootValueHandler((context, captured) -> {
            option[0] = context.option();
            value[0] = captured;
        }, "<assignment>", "Store a config assignment.");
        parser.addInlineParser(config);
        parser.addAlias("-c", "--config", "user-file=/tmp/user.json");
        parser.parseOrThrow(argv.length, argv);

        Assertions.expectEquals(option[0], "--config", "inline root option should be preserved");
        Assertions.expectEquals(value[0], "user-file=/tmp/user.json", "preset value should reach root handler");
    }

    private static void testAliasPresetTokensRejectedForFlags() {
        String[] argv = {"prog", "-v"};
        Parser parser = new Parser();
        parser.addAlias("-v", "--verbose", "unexpected");
        parser.setHandler("--verbose", context -> {
        }, "Enable verbose logging.");

        CliError error = Assertions.expectThrows(
            CliError.class,
            () -> parser.parseOrThrow(argv.length, argv),
            "aliases with preset tokens must not target flags");

        Assertions.expectEquals(error.option(), "-v", "error should surface the alias token");
        Assertions.expectContains(error.getMessage(), "does not accept values", "error should explain flag preset rejection");
    }

    private static void testRequiredValueAcceptsOptionLikeFirstToken() {
        String[] argv = {"prog", "--output", "-v"};
        final String[] value = {""};

        Parser parser = new Parser();
        parser.addAlias("-v", "--verbose");
        parser.setHandler("--output", (context, captured) -> value[0] = captured, "Set output target.");
        parser.setHandler("--verbose", context -> {
            throw new AssertionError("verbose alias should not be treated as an option value");
        }, "Enable verbose logging.");
        parser.parseOrThrow(argv.length, argv);

        Assertions.expectEquals(value[0], "-v", "required values should accept option-like first tokens");
    }

    private static void testBareInlineRootPrintsHelp() throws Exception {
        String[] argv = {"prog", "--alpha"};
        Parser parser = new Parser();
        InlineParser alpha = new InlineParser("--alpha");
        alpha.setOptionalValueHandler("-enable", (context, value) -> {
        }, "Enable alpha processing.");
        parser.addInlineParser(alpha);

        String stdout = TestSupport.captureStdout(() -> parser.parseOrThrow(argv.length, argv));

        Assertions.expectContains(stdout, "Available --alpha-* options:", "bare inline root should print help");
        Assertions.expectContains(stdout, "--alpha-enable [value]", "help should include optional value syntax");
    }

    private static void testDoubleDashRemainsUnknown() {
        String[] argv = {"prog", "--", "-v"};
        Parser parser = new Parser();
        parser.addAlias("-v", "--verbose");
        parser.setHandler("--verbose", context -> {
        }, "Enable verbose logging.");

        CliError error = Assertions.expectThrows(
            CliError.class,
            () -> parser.parseOrThrow(argv.length, argv),
            "double dash should remain an unknown option");

        Assertions.expectEquals(error.option(), "--", "double dash should be reported as the failing option");
    }
}
