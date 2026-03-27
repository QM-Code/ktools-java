package ktrace.tests;

import java.util.List;

import ktrace.Logger;
import ktrace.OutputOptions;
import ktrace.TraceColors;
import ktrace.TraceLogger;

final class ApiTests {
    private ApiTests() {
    }

    static void run() throws Exception {
        testFormatMessage();
        testWarnLoggingOutput();
        testTraceOutput();
        testSelectorSemantics();
        testConflictingColorsRejected();
        testTraceChangedSuppressesDuplicates();
    }

    private static void testFormatMessage() {
        Assertions.expectEquals(
            ktrace.internal.TraceInternals.formatMessage("value {} {}", 7, "done"),
            "value 7 done",
            "formatMessage should replace ordered placeholders");
        Assertions.expectEquals(
            ktrace.internal.TraceInternals.formatMessage("escaped {{}}"),
            "escaped {}",
            "formatMessage should preserve escaped braces");
        Assertions.expectEquals(
            ktrace.internal.TraceInternals.formatMessage("bool {}", true),
            "bool true",
            "formatMessage should stringify booleans");

        Assertions.expectThrows(
            IllegalArgumentException.class,
            () -> ktrace.internal.TraceInternals.formatMessage("value {} {}", 7),
            "missing arguments should fail");
        Assertions.expectThrows(
            IllegalArgumentException.class,
            () -> ktrace.internal.TraceInternals.formatMessage("{:x}", 7),
            "unsupported tokens should fail");
    }

    private static void testWarnLoggingOutput() throws Exception {
        Logger logger = new Logger();
        TraceLogger trace = new TraceLogger("tests");
        logger.addTraceLogger(trace);
        logger.setOutputOptions(new OutputOptions(true, true, false, false));

        String text = TestSupport.captureStdout(() -> {
            trace.info("info message");
            trace.warn("warn value {}", 7);
            trace.error("error message");
        });

        Assertions.expectContains(text, "[tests] [info]", "info output should include namespace/severity");
        Assertions.expectContains(text, "[tests] [warning]", "warn output should include severity");
        Assertions.expectContains(text, "[tests] [error]", "error output should include severity");
        Assertions.expectContains(text, "warn value 7", "warn output should include formatted value");
        Assertions.expectContains(text, "[ApiTests:", "output should include source labels when enabled");
    }

    private static void testTraceOutput() throws Exception {
        Logger logger = new Logger();
        TraceLogger trace = new TraceLogger("tests");
        trace.addChannel("trace", TraceColors.color("Gold3"));
        logger.addTraceLogger(trace);
        logger.enableChannel("tests.trace");

        String text = TestSupport.captureStdout(() -> trace.trace("trace", "member {} {{ok}}", 42));
        Assertions.expectContains(text, "[tests] [trace]", "trace output should include channel");
        Assertions.expectContains(text, "member 42 {ok}", "trace output should format messages");
    }

    private static void testSelectorSemantics() {
        Logger logger = new Logger();
        TraceLogger trace = new TraceLogger("tests");
        trace.addChannel("net");
        trace.addChannel("cache");
        trace.addChannel("store");
        trace.addChannel("store.requests");
        logger.addTraceLogger(trace);

        logger.enableChannels("tests.*");
        Assertions.expect(logger.shouldTraceChannel("tests.net"), "tests.net should be enabled by tests.*");
        Assertions.expect(logger.shouldTraceChannel("tests.cache"), "tests.cache should be enabled by tests.*");

        logger.disableChannels("tests.*");
        Assertions.expect(!logger.shouldTraceChannel("tests.net"), "tests.net should be disabled by tests.*");

        logger.enableChannel("tests.net");
        Assertions.expect(logger.shouldTraceChannel("tests.net"), "tests.net should be explicitly re-enabled");
        Assertions.expect(!logger.shouldTraceChannel("tests.cache"), "tests.cache should stay disabled");

        logger.enableChannels("*.*.*.*");
        Assertions.expect(logger.shouldTraceChannel("tests.store.requests"), "depth3 wildcard should enable nested channel");
        Assertions.expect(!logger.shouldTraceChannel("tests.bad name"), "invalid names should never trace");
    }

    private static void testConflictingColorsRejected() {
        Logger logger = new Logger();

        TraceLogger first = new TraceLogger("tests");
        first.addChannel("net");
        logger.addTraceLogger(first);

        TraceLogger explicitColor = new TraceLogger("tests");
        explicitColor.addChannel("net", TraceColors.color("Gold3"));
        logger.addTraceLogger(explicitColor);

        TraceLogger conflicting = new TraceLogger("tests");
        conflicting.addChannel("net", TraceColors.color("Orange3"));
        Assertions.expectThrows(
            IllegalArgumentException.class,
            () -> logger.addTraceLogger(conflicting),
            "conflicting colors should be rejected");
    }

    private static void testTraceChangedSuppressesDuplicates() throws Exception {
        Logger logger = new Logger();
        TraceLogger trace = new TraceLogger("tests");
        trace.addChannel("changed");
        logger.addTraceLogger(trace);
        logger.enableChannel("tests.changed");

        String text = TestSupport.captureStdout(() -> {
            emitChanged(trace, "key-1");
            emitChanged(trace, "key-1");
            emitChanged(trace, "key-2");
        });

        long count = List.of(text.split("\n")).stream().filter(line -> line.contains("changed")).count();
        Assertions.expectEquals(count, 2L, "traceChanged should suppress duplicate keys from the same call site");
    }

    private static void emitChanged(TraceLogger trace, String key) {
        trace.traceChanged("changed", key, "changed");
    }
}
