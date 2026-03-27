package kcli.demo.alpha;

import kcli.InlineParser;

import static kcli.demo.common.DemoSupport.printProcessingLine;

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
}
