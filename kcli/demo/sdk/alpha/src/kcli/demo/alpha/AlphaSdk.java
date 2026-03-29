package kcli.demo.alpha;

import java.util.stream.Collectors;

import kcli.HandlerContext;
import kcli.InlineParser;

public final class AlphaSdk {
    private AlphaSdk() {
    }

    public static InlineParser getInlineParser() {
        InlineParser parser = new InlineParser("--alpha");
        parser.setHandler("-message",
                          (context, value) -> printProcessingLine(context, value),
                          "Set alpha message label.");
        parser.setOptionalValueHandler("-enable",
                                       (context, value) -> printProcessingLine(context, value),
                                       "Enable alpha processing.");
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
