package kcli.demo.omega;

import kcli.InlineParser;
import kcli.Parser;

import kcli.demo.alpha.AlphaSdk;
import kcli.demo.beta.BetaSdk;
import kcli.demo.gamma.GammaSdk;

import static kcli.demo.common.DemoSupport.withProgram;

public final class Main {
    private Main() {
    }

    public static void main(String[] args) {
        String[] argv = withProgram("kcli_demo_omega", args);

        Parser parser = new Parser();
        InlineParser alphaParser = AlphaSdk.getInlineParser();
        InlineParser betaParser = BetaSdk.getInlineParser();
        InlineParser gammaParser = GammaSdk.getInlineParser();
        InlineParser buildParser = new InlineParser("--build");

        gammaParser.setRoot("--newgamma");

        buildParser.setHandler("-profile", (context, value) -> {
        }, "Set build profile.");
        buildParser.setHandler("-clean", context -> {
        }, "Enable clean build.");

        parser.addInlineParser(alphaParser);
        parser.addInlineParser(betaParser);
        parser.addInlineParser(gammaParser);
        parser.addInlineParser(buildParser);

        parser.addAlias("-v", "--verbose");
        parser.addAlias("-out", "--output");
        parser.addAlias("-a", "--alpha-enable");
        parser.addAlias("-b", "--build-profile");

        parser.setHandler("--verbose", context -> {
        }, "Enable verbose app logging.");
        parser.setHandler("--output", (context, value) -> {
        }, "Set app output target.");
        parser.setPositionalHandler(context -> {
        });

        parser.parseOrExit(argv.length, argv);

        System.out.println();
        System.out.println("Usage:");
        System.out.println("  kcli_demo_omega --<root>");
        System.out.println();
        System.out.println("Enabled --<root> prefixes:");
        System.out.println("  --alpha");
        System.out.println("  --beta");
        System.out.println("  --newgamma (gamma override)");
        System.out.println();
    }
}
