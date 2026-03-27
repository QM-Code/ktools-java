package kcli.internal;

import java.util.LinkedHashMap;

import kcli.PositionalHandler;

public final class ParserData {
    PositionalHandler positionalHandler;
    final LinkedHashMap<String, AliasBinding> aliases = new LinkedHashMap<>();
    final LinkedHashMap<String, CommandBinding> commands = new LinkedHashMap<>();
    final LinkedHashMap<String, InlineParserData> inlineParsers = new LinkedHashMap<>();

    public ParserData() {
    }
}
