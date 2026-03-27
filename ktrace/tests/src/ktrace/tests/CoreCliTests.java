package ktrace.tests;

final class CoreCliTests {
    private CoreCliTests() {
    }

    static void run() throws Exception {
        testUnknownOption();
        testBareTraceRoot();
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
