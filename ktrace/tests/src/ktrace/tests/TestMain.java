package ktrace.tests;

public final class TestMain {
    private TestMain() {
    }

    public static void main(String[] args) throws Exception {
        ApiTests.run();
        CoreCliTests.run();
        OmegaCliTests.run();
        System.out.println("Java ktrace tests passed.");
    }
}
