package kcli.demo.gamma;

import kcli.InlineParser;

import static kcli.demo.common.DemoSupport.printProcessingLine;

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
}
