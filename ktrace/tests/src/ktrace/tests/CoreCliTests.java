package ktrace.tests;

final class CoreCliTests {
    private CoreCliTests() {
    }

    static void run() throws Exception {
        testUnknownOption();
        testBareTraceRoot();
        testExamplesOption();
        testNamespacesOption();
        testChannelsOption();
        testColorsOption();
        testTimestampsOption();
        testImportedSelector();
    }

    private static void testUnknownOption() throws Exception {
        TestSupport.ProcessResult result = TestSupport.runJava("ktrace.demo.core.Main", "--trace-f");
        Assertions.expect(result.exitCode() != 0, "unknown trace option should fail");
        Assertions.expectContains(result.stderr(), "[error] [cli] unknown option --trace-f", "unknown option should be reported");
    }

    private static void testBareTraceRoot() throws Exception {
        TestSupport.ProcessResult result = TestSupport.runJava("ktrace.demo.core.Main", "--trace");
        Assertions.expectEquals(result.exitCode(), 0, "bare trace root should print help");
        Assertions.expectContains(result.stdout(), "Available --trace-* options:", "trace root should print help");
        Assertions.expectContains(result.stdout(), "--trace <channels>", "trace help should include root value");
        Assertions.expectNotContains(result.stdout(), "Trace selector examples:", "bare root should not print examples");
    }

    private static void testExamplesOption() throws Exception {
        TestSupport.ProcessResult result = TestSupport.runJava("ktrace.demo.core.Main", "--trace-examples");
        Assertions.expectEquals(result.exitCode(), 0, "trace examples should succeed");
        Assertions.expectContains(result.stdout(), "General trace selector pattern:",
            "trace examples should print the general selector form");
        Assertions.expectContains(result.stdout(), "*.net",
            "trace examples should include wildcard selector examples");
    }

    private static void testNamespacesOption() throws Exception {
        TestSupport.ProcessResult result = TestSupport.runJava("ktrace.demo.core.Main", "--trace-namespaces");
        Assertions.expectEquals(result.exitCode(), 0, "trace namespaces should succeed");
        Assertions.expectContains(result.stdout(), "Available trace namespaces:",
            "trace namespaces should print a heading");
        Assertions.expectContains(result.stdout(), "core", "local core namespace should be listed");
        Assertions.expectContains(result.stdout(), "alpha", "imported alpha namespace should be listed");
    }

    private static void testChannelsOption() throws Exception {
        TestSupport.ProcessResult result = TestSupport.runJava("ktrace.demo.core.Main", "--trace-channels");
        Assertions.expectEquals(result.exitCode(), 0, "trace channels should succeed");
        Assertions.expectContains(result.stdout(), "Available trace channels:",
            "trace channels should print a heading");
        Assertions.expectContains(result.stdout(), "core.app", "local channels should be listed");
        Assertions.expectContains(result.stdout(), "alpha.net", "imported channels should be listed");
    }

    private static void testColorsOption() throws Exception {
        TestSupport.ProcessResult result = TestSupport.runJava("ktrace.demo.core.Main", "--trace-colors");
        Assertions.expectEquals(result.exitCode(), 0, "trace colors should succeed");
        Assertions.expectContains(result.stdout(), "Available trace colors:",
            "trace colors should print a heading");
        Assertions.expectContains(result.stdout(), "BrightCyan",
            "trace colors should include named color options");
    }

    private static void testTimestampsOption() throws Exception {
        TestSupport.ProcessResult result =
            TestSupport.runJava("ktrace.demo.core.Main", "--trace", ".app", "--trace-timestamps");
        Assertions.expectEquals(result.exitCode(), 0, "timestamps option should succeed");
        Assertions.expectContains(result.stdout(), "[core] [", "trace output should include namespace and timestamp prefix");
        Assertions.expectContains(result.stdout(), "] [app] cli processing enabled, use --trace for options", "trace output should include app channel");
    }

    private static void testImportedSelector() throws Exception {
        TestSupport.ProcessResult result = TestSupport.runJava("ktrace.demo.core.Main", "--trace", "*.*");
        Assertions.expectEquals(result.exitCode(), 0, "imported selector should succeed");
        Assertions.expectContains(result.stdout(), "[core] [app] cli processing enabled, use --trace for options",
            "local trace output should be visible");
        Assertions.expectContains(result.stdout(), "[alpha] [net] testing...", "imported alpha traces should be visible");
    }
}
