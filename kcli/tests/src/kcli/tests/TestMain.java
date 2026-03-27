package kcli.tests;

public final class TestMain {
    private TestMain() {
    }

    public static void main(String[] args) throws Exception {
        ApiTests.run();
        BootstrapCliTests.run();
        CoreCliTests.run();
        OmegaCliTests.run();
        System.out.println("Java tests passed.");
    }
}
