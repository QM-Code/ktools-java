package ktrace.tests;

final class BootstrapCliTests {
    private BootstrapCliTests() {
    }

    static void run() throws Exception {
        testBootstrapTraceOutput();
    }

    private static void testBootstrapTraceOutput() throws Exception {
        TestSupport.ProcessResult result = TestSupport.runJava("ktrace.demo.bootstrap.Main");
        Assertions.expectEquals(result.exitCode(), 0, "bootstrap demo should succeed");
        Assertions.expectContains(result.stdout(),
            "[bootstrap] [app] KTRACE java bootstrap import/parse check passed",
            "bootstrap demo should emit its trace output");
    }
}
