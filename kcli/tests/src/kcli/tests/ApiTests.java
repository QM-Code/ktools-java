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
        testInlineParserRejectsInvalidRoot();
        testUnknownOptionDoesNotRunHandlers();
        testAliasRewritesOption();
        testAliasPresetTokensPrependUserValue();
        testAliasPresetTokensSatisfyRequiredValue();
        testAliasPresetTokensApplyToInlineRootValue();
        testAliasPresetTokensRejectedForFlags();
        testAliasDoesNotRewriteRequiredValueTokens();
        testParserCanBeReusedAcrossParses();
        testAliasValidation();
        testPositionalHandlerRequiresNonEmpty();
        testEndUserHandlerRejectsSingleDash();
        testInlineHandlerShortAndFullForms();
        testInlineHandlerRejectsWrongRoot();
        testRequiredValueAcceptsOptionLikeFirstToken();
        testBareInlineRootPrintsHelp();
        testInlineRootValueHelpRowPrints();
        testInlineRootValueJoinsTokens();
        testMissingInlineRootValueHandlerErrors();
        testOptionalValueAllowsMissingValue();
        testOptionalValueAcceptsExplicitEmptyValue();
        testFlagHandlerDoesNotConsumeFollowingToken();
        testRequiredValueRejectsMissingValue();
        testRequiredValuePreservesWhitespace();
        testRequiredValueAcceptsExplicitEmptyValue();
        testPositionalsPreserveExplicitEmptyTokens();
        testUnknownInlineOptionThrowsCliError();
        testOptionHandlerExceptionBecomesCliError();
        testPositionalHandlerExceptionBecomesCliError();
        testSinglePassMixedInlineEndUserAndPositionals();
        testInlineParserRootOverrideApplies();
        testDuplicateInlineRootRejected();
        testDoubleDashRemainsUnknown();
    }

    private static void testParserEmptyParseSucceeds() {
        String[] argv = {"prog"};
        Parser parser = new Parser();
        parser.parseOrThrow(argv.length, argv);
        Assertions.expectEquals(List.of(argv), List.of("prog"), "parseOrThrow should leave argv unchanged");
    }

    private static void testInlineParserRejectsInvalidRoot() {
        IllegalArgumentException error = Assertions.expectThrows(
            IllegalArgumentException.class,
            () -> new InlineParser("-build"),
            "single-dash inline roots should be rejected");

        Assertions.expectContains(error.getMessage(), "must use '--root' or 'root'",
            "invalid inline roots should surface the normalization guidance");
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

    private static void testAliasPresetTokensPrependUserValue() {
        String[] argv = {"prog", "-c", "settings.json"};
        final String[] option = {""};
        final String[] value = {""};
        final List<String> tokens = new ArrayList<>();

        Parser parser = new Parser();
        parser.addAlias("-c", "--config-load", "user-file");
        parser.setHandler("--config-load", (context, captured) -> {
            option[0] = context.option();
            value[0] = captured;
            tokens.addAll(context.valueTokens());
        }, "Load config.");

        parser.parseOrThrow(argv.length, argv);

        Assertions.expectEquals(option[0], "--config-load", "preset alias should target canonical option");
        Assertions.expectEquals(value[0], "user-file settings.json",
            "preset alias should prepend configured value tokens");
        Assertions.expectEquals(tokens, List.of("user-file", "settings.json"),
            "valueTokens should include preset and user-supplied tokens");
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

    private static void testAliasDoesNotRewriteRequiredValueTokens() {
        String[] argv = {"prog", "--output", "-v"};
        final boolean[] verbose = {false};
        final String[] output = {""};

        Parser parser = new Parser();
        parser.addAlias("-v", "--verbose");
        parser.setHandler("--output", (context, captured) -> output[0] = captured, "Set output target.");
        parser.setHandler("--verbose", context -> verbose[0] = true, "Enable verbose logging.");

        parser.parseOrThrow(argv.length, argv);

        Assertions.expect(!verbose[0], "alias-like required values should not dispatch alias handlers");
        Assertions.expectEquals(output[0], "-v", "required values should preserve raw alias-like tokens");
        Assertions.expectEquals(List.of(argv), List.of("prog", "--output", "-v"),
            "parseOrThrow should leave argv unchanged when required values look like aliases");
    }

    private static void testParserCanBeReusedAcrossParses() {
        String[] first = {"prog", "-v"};
        String[] second = {"prog", "-v"};
        final int[] calls = {0};

        Parser parser = new Parser();
        parser.addAlias("-v", "--verbose");
        parser.setHandler("--verbose", context -> calls[0] += 1, "Enable verbose logging.");

        parser.parseOrThrow(first.length, first);
        parser.parseOrThrow(second.length, second);

        Assertions.expectEquals(calls[0], 2, "reused parsers should dispatch handlers on each parse");
        Assertions.expectEquals(List.of(first), List.of("prog", "-v"), "first argv should remain unchanged");
        Assertions.expectEquals(List.of(second), List.of("prog", "-v"), "second argv should remain unchanged");
    }

    private static void testAliasValidation() {
        Parser parser = new Parser();

        IllegalArgumentException invalidAlias = Assertions.expectThrows(
            IllegalArgumentException.class,
            () -> parser.addAlias("--verbose", "--output"),
            "double-dash aliases should be rejected");
        Assertions.expectContains(invalidAlias.getMessage(), "single-dash form",
            "alias validation should explain the expected alias form");

        IllegalArgumentException invalidTarget = Assertions.expectThrows(
            IllegalArgumentException.class,
            () -> parser.addAlias("-v", "--bad target"),
            "whitespace in alias targets should be rejected");
        Assertions.expectContains(invalidTarget.getMessage(), "double-dash form",
            "target validation should explain the expected target form");

        IllegalArgumentException singleDashTarget = Assertions.expectThrows(
            IllegalArgumentException.class,
            () -> parser.addAlias("-a", "-b"),
            "single-dash alias targets should be rejected");
        Assertions.expectContains(singleDashTarget.getMessage(), "double-dash form",
            "single-dash targets should surface the double-dash requirement");
    }

    private static void testPositionalHandlerRequiresNonEmpty() {
        Parser parser = new Parser();
        IllegalArgumentException error = Assertions.expectThrows(
            IllegalArgumentException.class,
            () -> parser.setPositionalHandler(null),
            "null positional handlers should be rejected");

        Assertions.expectContains(error.getMessage(), "must not be empty",
            "null positional handlers should explain the contract");
    }

    private static void testEndUserHandlerRejectsSingleDash() {
        Parser parser = new Parser();
        IllegalArgumentException error = Assertions.expectThrows(
            IllegalArgumentException.class,
            () -> parser.setHandler("-verbose", context -> {
            }, "Enable verbose logging."),
            "single-dash end-user options should be rejected");

        Assertions.expectContains(error.getMessage(), "end-user handler option must use '--name' or 'name'",
            "single-dash end-user options should surface the normalization guidance");
    }

    private static void testInlineHandlerShortAndFullForms() {
        String[] argv = {"prog", "--build-flag", "--build-value", "data"};
        final boolean[] flag = {false};
        final String[] value = {""};

        Parser parser = new Parser();
        InlineParser build = new InlineParser("build");
        build.setHandler("-flag", context -> flag[0] = true, "Enable build flag.");
        build.setHandler("--build-value", (context, captured) -> value[0] = captured, "Set build value.");
        parser.addInlineParser(build);

        parser.parseOrThrow(argv.length, argv);

        Assertions.expect(flag[0], "short-form inline handler should dispatch");
        Assertions.expectEquals(value[0], "data", "fully-qualified inline handler should dispatch");
    }

    private static void testInlineHandlerRejectsWrongRoot() {
        InlineParser build = new InlineParser("--build");
        IllegalArgumentException error = Assertions.expectThrows(
            IllegalArgumentException.class,
            () -> build.setHandler("--other-flag", context -> {
            }, "Enable other flag."),
            "mismatched fully-qualified inline options should be rejected");

        Assertions.expectContains(error.getMessage(), "inline handler option must use '-name' or '--build-name'",
            "mismatched roots should surface the valid inline forms");
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

    private static void testInlineRootValueHelpRowPrints() throws Exception {
        String[] argv = {"prog", "--build"};
        Parser parser = new Parser();
        InlineParser build = new InlineParser("--build");
        build.setRootValueHandler((context, captured) -> {
        }, "<selector>", "Select build targets.");
        build.setHandler("-flag", context -> {
        }, "Enable build flag.");
        parser.addInlineParser(build);

        String stdout = TestSupport.captureStdout(() -> parser.parseOrThrow(argv.length, argv));

        Assertions.expectContains(stdout, "--build <selector>", "root value help should include the bare root form");
        Assertions.expectContains(stdout, "Select build targets.", "root value help should include its description");
    }

    private static void testInlineRootValueJoinsTokens() {
        String[] argv = {"prog", "--build", "fast", "mode"};
        final String[] option = {""};
        final String[] value = {""};
        final List<String> tokens = new ArrayList<>();

        Parser parser = new Parser();
        InlineParser build = new InlineParser("--build");
        build.setRootValueHandler((context, captured) -> {
            option[0] = context.option();
            value[0] = captured;
            tokens.addAll(context.valueTokens());
        });
        parser.addInlineParser(build);

        parser.parseOrThrow(argv.length, argv);

        Assertions.expectEquals(option[0], "--build", "root value handlers should observe the root option");
        Assertions.expectEquals(value[0], "fast mode", "root value handlers should receive joined values");
        Assertions.expectEquals(tokens, List.of("fast", "mode"),
            "root value handlers should receive tokenized value parts");
    }

    private static void testMissingInlineRootValueHandlerErrors() {
        String[] argv = {"prog", "--build", "fast"};

        Parser parser = new Parser();
        parser.addInlineParser(new InlineParser("--build"));

        CliError error = Assertions.expectThrows(
            CliError.class,
            () -> parser.parseOrThrow(argv.length, argv),
            "bare roots with values should fail when no root value handler is registered");

        Assertions.expectEquals(error.option(), "--build", "missing root value handlers should report the root option");
        Assertions.expectContains(error.getMessage(), "unknown value for option '--build'",
            "missing root value handlers should explain the failure");
    }

    private static void testOptionalValueAllowsMissingValue() {
        String[] argv = {"prog", "--build-enable"};
        final boolean[] called = {false};
        final String[] value = {"not-empty"};
        final List<String> tokens = new ArrayList<>();

        Parser parser = new Parser();
        InlineParser build = new InlineParser("--build");
        build.setOptionalValueHandler("-enable", (context, captured) -> {
            called[0] = true;
            value[0] = captured;
            tokens.addAll(context.valueTokens());
        }, "Enable build mode.");
        parser.addInlineParser(build);

        parser.parseOrThrow(argv.length, argv);

        Assertions.expect(called[0], "optional value handlers should run when the value is omitted");
        Assertions.expectEquals(value[0], "", "missing optional values should surface as empty strings");
        Assertions.expectEquals(tokens, List.of(), "missing optional values should not invent token parts");
        Assertions.expectEquals(List.of(argv), List.of("prog", "--build-enable"),
            "parseOrThrow should leave argv unchanged for missing optional values");
    }

    private static void testOptionalValueAcceptsExplicitEmptyValue() {
        String[] argv = {"prog", "--build-enable", ""};
        final String[] value = {null};
        final List<String> tokens = new ArrayList<>();

        Parser parser = new Parser();
        InlineParser build = new InlineParser("--build");
        build.setOptionalValueHandler("-enable", (context, captured) -> {
            value[0] = captured;
            tokens.addAll(context.valueTokens());
        }, "Enable build mode.");
        parser.addInlineParser(build);

        parser.parseOrThrow(argv.length, argv);

        Assertions.expectEquals(value[0], "", "explicit empty optional values should stay empty");
        Assertions.expectEquals(tokens, List.of(""), "optional value handlers should receive explicit empty tokens");
    }

    private static void testFlagHandlerDoesNotConsumeFollowingToken() {
        String[] argv = {"prog", "--build-meta", "data"};
        final boolean[] flag = {false};
        final List<String> positionals = new ArrayList<>();

        Parser parser = new Parser();
        InlineParser build = new InlineParser("--build");
        build.setHandler("-meta", context -> flag[0] = true, "Record metadata.");
        parser.addInlineParser(build);
        parser.setPositionalHandler(context -> positionals.addAll(context.valueTokens()));

        parser.parseOrThrow(argv.length, argv);

        Assertions.expect(flag[0], "flag handlers should still run");
        Assertions.expectEquals(positionals, List.of("data"),
            "flag handlers should leave following non-option tokens as positionals");
    }

    private static void testRequiredValueRejectsMissingValue() {
        String[] argv = {"prog", "--build-value"};

        Parser parser = new Parser();
        InlineParser build = new InlineParser("--build");
        build.setHandler("-value", (context, captured) -> {
        }, "Set build value.");
        parser.addInlineParser(build);

        CliError error = Assertions.expectThrows(
            CliError.class,
            () -> parser.parseOrThrow(argv.length, argv),
            "required inline values should fail when missing");

        Assertions.expectEquals(error.option(), "--build-value",
            "missing required values should report the failing option");
        Assertions.expectContains(error.getMessage(), "requires a value",
            "missing required values should explain the contract");
    }

    private static void testRequiredValuePreservesWhitespace() {
        String[] argv = {"prog", "--name", " Joe "};
        final String[] value = {""};
        final List<String> tokens = new ArrayList<>();

        Parser parser = new Parser();
        parser.setHandler("--name", (context, captured) -> {
            value[0] = captured;
            tokens.addAll(context.valueTokens());
        }, "Set the display name.");

        parser.parseOrThrow(argv.length, argv);

        Assertions.expectEquals(value[0], " Joe ", "required values should preserve shell-provided whitespace");
        Assertions.expectEquals(tokens, List.of(" Joe "), "valueTokens should preserve shell-provided whitespace");
    }

    private static void testRequiredValueAcceptsExplicitEmptyValue() {
        String[] argv = {"prog", "--name", ""};
        final String[] value = {"not-empty"};
        final List<String> tokens = new ArrayList<>();

        Parser parser = new Parser();
        parser.setHandler("--name", (context, captured) -> {
            value[0] = captured;
            tokens.addAll(context.valueTokens());
        }, "Set the display name.");

        parser.parseOrThrow(argv.length, argv);

        Assertions.expectEquals(value[0], "", "required values should accept explicit empty strings");
        Assertions.expectEquals(tokens, List.of(""), "valueTokens should preserve explicit empty required values");
    }

    private static void testPositionalsPreserveExplicitEmptyTokens() {
        String[] argv = {"prog", "", "tail"};
        final List<String> positionals = new ArrayList<>();

        Parser parser = new Parser();
        parser.setPositionalHandler(context -> positionals.addAll(context.valueTokens()));
        parser.parseOrThrow(argv.length, argv);

        Assertions.expectEquals(positionals, List.of("", "tail"),
            "positional handlers should receive explicit empty shell tokens");
    }

    private static void testUnknownInlineOptionThrowsCliError() {
        String[] argv = {"prog", "--build-unknown"};

        Parser parser = new Parser();
        parser.addInlineParser(new InlineParser("--build"));

        CliError error = Assertions.expectThrows(
            CliError.class,
            () -> parser.parseOrThrow(argv.length, argv),
            "unknown inline options should surface CliError");

        Assertions.expectEquals(error.option(), "--build-unknown", "unknown inline options should report the token");
        Assertions.expectContains(error.getMessage(), "unknown option --build-unknown",
            "unknown inline options should preserve the standard message");
    }

    private static void testOptionHandlerExceptionBecomesCliError() {
        String[] argv = {"prog", "--verbose"};

        Parser parser = new Parser();
        parser.setHandler("--verbose", context -> {
            throw new RuntimeException("option boom");
        }, "Enable verbose logging.");

        CliError error = Assertions.expectThrows(
            CliError.class,
            () -> parser.parseOrThrow(argv.length, argv),
            "option handler exceptions should be wrapped as CliError");

        Assertions.expectEquals(error.option(), "--verbose", "handler failures should report the option");
        Assertions.expectContains(error.getMessage(), "option boom", "handler failures should preserve the message");
        Assertions.expectContains(error.getMessage(), "--verbose",
            "handler failures should include the effective option token");
    }

    private static void testPositionalHandlerExceptionBecomesCliError() {
        String[] argv = {"prog", "tail"};

        Parser parser = new Parser();
        parser.setPositionalHandler(context -> {
            throw new RuntimeException("positional boom");
        });

        CliError error = Assertions.expectThrows(
            CliError.class,
            () -> parser.parseOrThrow(argv.length, argv),
            "positional handler exceptions should be wrapped as CliError");

        Assertions.expectEquals(error.option(), "", "positional failures should not report an option token");
        Assertions.expectContains(error.getMessage(), "positional boom",
            "positional failures should preserve the thrown message");
    }

    private static void testSinglePassMixedInlineEndUserAndPositionals() {
        String[] argv = {"prog", "tail", "--alpha-message", "hello", "--output", "stdout"};
        final String[] alphaMessage = {""};
        final String[] output = {""};
        final List<String> positionals = new ArrayList<>();

        Parser parser = new Parser();
        InlineParser alpha = new InlineParser("--alpha");
        alpha.setHandler("-message", (context, captured) -> alphaMessage[0] = captured, "Set alpha message.");
        parser.addInlineParser(alpha);
        parser.setHandler("--output", (context, captured) -> output[0] = captured, "Set output target.");
        parser.setPositionalHandler(context -> positionals.addAll(context.valueTokens()));

        parser.parseOrThrow(argv.length, argv);

        Assertions.expectEquals(alphaMessage[0], "hello", "inline options should dispatch in the same parse pass");
        Assertions.expectEquals(output[0], "stdout", "top-level options should dispatch in the same parse pass");
        Assertions.expectEquals(positionals, List.of("tail"), "remaining positionals should be preserved");
    }

    private static void testInlineParserRootOverrideApplies() {
        String[] argv = {"prog", "--newgamma-tag", "prod"};
        final String[] tag = {""};

        Parser parser = new Parser();
        InlineParser gamma = new InlineParser("--gamma");
        gamma.setHandler("-tag", (context, captured) -> tag[0] = captured, "Set gamma tag.");
        gamma.setRoot("--newgamma");
        parser.addInlineParser(gamma);

        parser.parseOrThrow(argv.length, argv);

        Assertions.expectEquals(tag[0], "prod", "overridden inline roots should dispatch registered handlers");
    }

    private static void testDuplicateInlineRootRejected() {
        Parser parser = new Parser();
        parser.addInlineParser(new InlineParser("--build"));

        IllegalArgumentException error = Assertions.expectThrows(
            IllegalArgumentException.class,
            () -> parser.addInlineParser(new InlineParser("build")),
            "duplicate inline roots should be rejected");

        Assertions.expectContains(error.getMessage(), "already registered",
            "duplicate inline roots should identify the registration conflict");
    }

    private static void testDoubleDashRemainsUnknown() {
        String[] argv = {"prog", "--", "-v"};
        final boolean[] verbose = {false};
        Parser parser = new Parser();
        parser.addAlias("-v", "--verbose");
        parser.setHandler("--verbose", context -> verbose[0] = true, "Enable verbose logging.");

        CliError error = Assertions.expectThrows(
            CliError.class,
            () -> parser.parseOrThrow(argv.length, argv),
            "double dash should remain an unknown option");

        Assertions.expect(!verbose[0], "handlers should not run before the unknown double dash fails the parse");
        Assertions.expectEquals(error.option(), "--", "double dash should be reported as the failing option");
        Assertions.expectContains(error.getMessage(), "unknown option --",
            "double dash should preserve the standard unknown-option message");
        Assertions.expectEquals(List.of(argv), List.of("prog", "--", "-v"),
            "parseOrThrow should leave argv unchanged when double dash fails before alias expansion");
    }
}
