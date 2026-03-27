package kcli.demo.core;

import kcli.InlineParser;
import kcli.Parser;

import kcli.demo.alpha.AlphaSdk;

import static kcli.demo.common.DemoSupport.withProgram;

public final class Main {
    private Main() {
    }

    public static void main(String[] args) {
        String exeName = "kcli_demo_core";
        String[] argv = withProgram(exeName, args);

        Parser parser = new Parser();
        InlineParser alphaParser = AlphaSdk.getInlineParser();

        parser.addInlineParser(alphaParser);

        parser.addAlias("-v", "--verbose");
        parser.addAlias("-out", "--output");
        parser.addAlias("-a", "--alpha-enable");

        parser.setHandler("--verbose", context -> {
        }, "Enable verbose app logging.");
        parser.setHandler("--output", (context, value) -> {
        }, "Set app output target.");
        parser.parseOrExit(argv.length, argv);

        System.out.println();
        System.out.println("KCLI java demo core import/integration check passed");
        System.out.println();
        System.out.println("Usage:");
        System.out.println("  " + exeName + " --alpha");
        System.out.println("  " + exeName + " --output stdout");
        System.out.println();
        System.out.println("Enabled inline roots:");
        System.out.println("  --alpha");
        System.out.println();
    }
}
