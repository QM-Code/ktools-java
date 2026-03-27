package kcli.tests;

final class CoreCliTests {
    private CoreCliTests() {
    }

    static void run() throws Exception {
        testUnknownAlphaOption();
        testKnownAlphaOption();
        testAlphaOptionalNoValue();
        testAlphaHelpRoot();
        testOutputAliasOption();
        testDoubleDashNotSeparator();
    }

    private static void testUnknownAlphaOption() throws Exception {
        TestSupport.ProcessResult result = TestSupport.runJava("kcli.demo.core.Main", "--alpha-d");
        Assertions.expect(result.exitCode() != 0, "unknown alpha option should fail");
        Assertions.expectContains(result.stderr(), "[error] [cli] unknown option --alpha-d", "unknown alpha option should be reported");
        Assertions.expectNotContains(result.stdout(), "KCLI java demo core import/integration check passed", "failed parse should not print success");
    }

    private static void testKnownAlphaOption() throws Exception {
        TestSupport.ProcessResult result = TestSupport.runJava("kcli.demo.core.Main", "--alpha-message", "hello");
        Assertions.expectEquals(result.exitCode(), 0, "known alpha option should succeed");
        Assertions.expectContains(result.stdout(), "Processing --alpha-message with value \"hello\"", "alpha message should be processed");
    }

    private static void testAlphaOptionalNoValue() throws Exception {
        TestSupport.ProcessResult result = TestSupport.runJava("kcli.demo.core.Main", "--alpha-enable");
        Assertions.expectEquals(result.exitCode(), 0, "optional alpha flag should succeed");
        Assertions.expectContains(result.stdout(), "Processing --alpha-enable", "optional alpha flag should run without a value");
    }

    private static void testAlphaHelpRoot() throws Exception {
        TestSupport.ProcessResult result = TestSupport.runJava("kcli.demo.core.Main", "--alpha");
        Assertions.expectEquals(result.exitCode(), 0, "bare alpha root should print help");
        Assertions.expectContains(result.stdout(), "Available --alpha-* options:", "alpha help root should print a heading");
        Assertions.expectContains(result.stdout(), "--alpha-enable [value]", "alpha help should include optional value syntax");
    }

    private static void testOutputAliasOption() throws Exception {
        TestSupport.ProcessResult result = TestSupport.runJava("kcli.demo.core.Main", "-out", "stdout");
        Assertions.expectEquals(result.exitCode(), 0, "output alias should succeed");
        Assertions.expectContains(result.stdout(), "KCLI java demo core import/integration check passed", "output alias should reach the demo success path");
    }

    private static void testDoubleDashNotSeparator() throws Exception {
        TestSupport.ProcessResult result = TestSupport.runJava("kcli.demo.core.Main", "--", "--alpha-message", "hello");
        Assertions.expect(result.exitCode() != 0, "double dash should not be treated as a separator");
        Assertions.expectContains(result.stderr(), "[error] [cli] unknown option --", "double dash should be reported as unknown");
        Assertions.expectNotContains(result.stdout(), "Processing --alpha-message", "invalid parses should not execute handlers");
    }
}
