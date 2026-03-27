package kcli.tests;

final class Assertions {
    private Assertions() {
    }

    static void expect(boolean condition, String message) {
        if (!condition) {
            throw new AssertionError(message);
        }
    }

    static void expectEquals(Object actual, Object expected, String message) {
        if (expected == null ? actual == null : expected.equals(actual)) {
            return;
        }
        throw new AssertionError(message + "\nexpected: " + expected + "\nactual:   " + actual);
    }

    static void expectContains(String actual, String needle, String message) {
        if (actual != null && actual.contains(needle)) {
            return;
        }
        throw new AssertionError(message + "\nmissing:  " + needle + "\nactual:   " + actual);
    }

    static void expectNotContains(String actual, String needle, String message) {
        if (actual == null || !actual.contains(needle)) {
            return;
        }
        throw new AssertionError(message + "\nunexpected: " + needle + "\nactual:     " + actual);
    }

    static <T extends Throwable> T expectThrows(Class<T> type, ThrowingRunnable runnable, String message) {
        try {
            runnable.run();
        } catch (Throwable ex) {
            if (type.isInstance(ex)) {
                return type.cast(ex);
            }
            throw new AssertionError(message + "\nunexpected exception: " + ex, ex);
        }
        throw new AssertionError(message + "\nexpected exception: " + type.getName());
    }

    @FunctionalInterface
    interface ThrowingRunnable {
        void run() throws Exception;
    }
}
