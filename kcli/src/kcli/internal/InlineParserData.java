package kcli.internal;

import java.util.LinkedHashMap;

import kcli.ValueHandler;

public final class InlineParserData {
    String rootName = "";
    ValueHandler rootValueHandler;
    String rootValuePlaceholder = "";
    String rootValueDescription = "";
    final LinkedHashMap<String, CommandBinding> commands = new LinkedHashMap<>();

    public InlineParserData() {
    }

    public InlineParserData copy() {
        InlineParserData copy = new InlineParserData();
        copy.rootName = rootName;
        copy.rootValueHandler = rootValueHandler;
        copy.rootValuePlaceholder = rootValuePlaceholder;
        copy.rootValueDescription = rootValueDescription;
        for (var entry : commands.entrySet()) {
            copy.commands.put(entry.getKey(), entry.getValue().copy());
        }
        return copy;
    }

    public void copyFrom(InlineParserData other) {
        rootName = other.rootName;
        rootValueHandler = other.rootValueHandler;
        rootValuePlaceholder = other.rootValuePlaceholder;
        rootValueDescription = other.rootValueDescription;
        commands.clear();
        for (var entry : other.commands.entrySet()) {
            commands.put(entry.getKey(), entry.getValue().copy());
        }
    }
}
