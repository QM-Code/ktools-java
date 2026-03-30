package kcli.internal;

import java.util.LinkedHashMap;

import kcli.FlagHandler;
import kcli.ValueHandler;

public final class InlineParserData {
    String rootName = "";
    ValueHandler rootValueHandler;
    String rootValuePlaceholder = "";
    String rootValueDescription = "";
    final LinkedHashMap<String, CommandBinding> commands = new LinkedHashMap<>();

    public InlineParserData() {
    }

    public void setRoot(String root) {
        rootName = Normalization.normalizeInlineRootOptionOrThrow(root);
    }

    public void setRootValueHandler(ValueHandler handler) {
        if (handler == null) {
            throw new IllegalArgumentException("kcli root value handler must not be empty");
        }
        rootValueHandler = handler;
        rootValuePlaceholder = "";
        rootValueDescription = "";
    }

    public void setRootValueHandler(ValueHandler handler,
                                    String valuePlaceholder,
                                    String description) {
        if (handler == null) {
            throw new IllegalArgumentException("kcli root value handler must not be empty");
        }
        rootValueHandler = handler;
        rootValuePlaceholder = Normalization.normalizeHelpPlaceholderOrThrow(valuePlaceholder);
        rootValueDescription = Normalization.normalizeDescriptionOrThrow(description);
    }

    public void setHandler(String option, FlagHandler handler, String description) {
        String command = Normalization.normalizeInlineHandlerOptionOrThrow(option, rootName);
        commands.put(command, makeFlagBinding(handler, description));
    }

    public void setHandler(String option, ValueHandler handler, String description) {
        String command = Normalization.normalizeInlineHandlerOptionOrThrow(option, rootName);
        commands.put(command, makeValueBinding(handler, description, ValueArity.REQUIRED));
    }

    public void setOptionalValueHandler(String option,
                                        ValueHandler handler,
                                        String description) {
        String command = Normalization.normalizeInlineHandlerOptionOrThrow(option, rootName);
        commands.put(command, makeValueBinding(handler, description, ValueArity.OPTIONAL));
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

    private static CommandBinding makeFlagBinding(FlagHandler handler, String description) {
        if (handler == null) {
            throw new IllegalArgumentException("kcli flag handler must not be empty");
        }
        return CommandBinding.flag(handler, Normalization.normalizeDescriptionOrThrow(description));
    }

    private static CommandBinding makeValueBinding(ValueHandler handler,
                                                   String description,
                                                   ValueArity arity) {
        if (handler == null) {
            throw new IllegalArgumentException("kcli value handler must not be empty");
        }
        return CommandBinding.value(handler, Normalization.normalizeDescriptionOrThrow(description), arity);
    }
}
