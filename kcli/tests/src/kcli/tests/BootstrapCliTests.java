package kcli.tests;

final class BootstrapCliTests {
    private BootstrapCliTests() {
    }

    static void run() throws Exception {
        testVerboseAlias();
    }

    private static void testVerboseAlias() throws Exception {
        TestSupport.ProcessResult result = TestSupport.runJava("kcli.demo.bootstrap.Main", "-v");
        Assertions.expectEquals(result.exitCode(), 0, "bootstrap alias should succeed");
        Assertions.expectContains(result.stdout(), "Processing --verbose", "bootstrap should process the verbose alias");
        Assertions.expectContains(result.stdout(), "KCLI java bootstrap import/parse check passed", "bootstrap should report success");
    }
}
