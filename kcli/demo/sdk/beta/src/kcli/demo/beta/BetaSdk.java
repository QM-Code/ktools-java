package kcli.demo.beta;

import java.util.stream.Collectors;

import kcli.HandlerContext;
import kcli.InlineParser;

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

    private static void printProcessingLine(HandlerContext context, String value) {
        if (context.valueTokens().isEmpty()) {
            System.out.println("Processing " + context.option());
            return;
        }

        if (context.valueTokens().size() == 1) {
            System.out.println("Processing " + context.option() + " with value \"" + value + "\"");
            return;
        }

        String joined = context.valueTokens()
            .stream()
            .map(token -> "\"" + token + "\"")
            .collect(Collectors.joining(","));
        System.out.println("Processing " + context.option() + " with values [" + joined + "]");
    }
}
