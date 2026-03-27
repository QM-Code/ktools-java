package kcli.demo.beta;

import kcli.InlineParser;

import static kcli.demo.common.DemoSupport.printProcessingLine;

public final class BetaSdk {
    private BetaSdk() {
    }

    public static InlineParser getInlineParser() {
        InlineParser parser = new InlineParser("--beta");
        parser.setHandler("-profile",
                          (context, value) -> printProcessingLine(context, value),
                          "Select beta runtime profile.");
        parser.setHandler("-workers", (context, value) -> {
            try {
                Integer.parseInt(value);
            } catch (NumberFormatException ex) {
                throw new RuntimeException("expected an integer", ex);
            }
            printProcessingLine(context, value);
        }, "Set beta worker count.");
        return parser;
    }
}
