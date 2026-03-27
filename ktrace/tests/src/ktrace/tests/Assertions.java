package ktrace.tests;

final class Assertions {
    @FunctionalInterface
    interface ThrowingRunnable {
        void run() throws Exception;
    }

    private Assertions() {
    }

    static void expect(boolean condition, String message) {
        if (!condition) {
            throw new AssertionError(message);
        }
    }

    static void expectEquals(Object actual, Object expected, String message) {
        if ((actual == null && expected != null) || (actual != null && !actual.equals(expected))) {
            throw new AssertionError(message + " | actual=" + actual + " expected=" + expected);
        }
    }

    static void expectContains(String haystack, String needle, String message) {
        expect(haystack.contains(needle), message + " | missing=" + needle + " | output=\n" + haystack);
    }

    static void expectNotContains(String haystack, String needle, String message) {
        expect(!haystack.contains(needle), message + " | found=" + needle + " | output=\n" + haystack);
    }

    static <T extends Throwable> T expectThrows(Class<T> expectedType,
                                                ThrowingRunnable runnable,
                                                String message) {
        try {
            runnable.run();
        } catch (Throwable ex) {
            if (expectedType.isInstance(ex)) {
                return expectedType.cast(ex);
            }
            throw new AssertionError(message + " | unexpected exception " + ex, ex);
        }
        throw new AssertionError(message + " | expected exception " + expectedType.getName());
    }
}
