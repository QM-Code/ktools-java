package kcli.demo.bootstrap;

import kcli.Parser;

import static kcli.demo.common.DemoSupport.withProgram;

public final class Main {
    private Main() {
    }

    public static void main(String[] args) {
        String[] argv = withProgram("kcli_demo_bootstrap", args);
        Parser parser = new Parser();
        parser.addAlias("-v", "--verbose");
        parser.setHandler("--verbose",
                          context -> System.out.println("Processing " + context.option()),
                          "Enable verbose demo logging.");
        parser.parseOrExit(argv.length, argv);
        System.out.println();
        System.out.println("KCLI java bootstrap import/parse check passed");
        System.out.println();
    }
}
