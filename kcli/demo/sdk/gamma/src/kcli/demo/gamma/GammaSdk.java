package kcli.demo.gamma;

import java.util.stream.Collectors;

import kcli.HandlerContext;
import kcli.InlineParser;

public final class GammaSdk {
    private GammaSdk() {
    }

    public static InlineParser getInlineParser() {
        InlineParser parser = new InlineParser("--gamma");
        parser.setOptionalValueHandler("-strict",
                                       (context, value) -> printProcessingLine(context, value),
                                       "Enable strict gamma mode.");
        parser.setHandler("-tag",
                          (context, value) -> printProcessingLine(context, value),
                          "Set a gamma tag label.");
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
