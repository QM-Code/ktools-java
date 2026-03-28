package kcli.internal;

import java.util.ArrayList;
import java.util.List;

import kcli.FlagHandler;
import kcli.PositionalHandler;
import kcli.ValueHandler;

public final class Registration {
    private Registration() {
    }

    public static void setInlineRoot(InlineParserData data, String root) {
        data.rootName = Normalization.normalizeInlineRootOptionOrThrow(root);
    }

    public static void setRootValueHandler(InlineParserData data, ValueHandler handler) {
        if (handler == null) {
            throw new IllegalArgumentException("kcli root value handler must not be empty");
        }
        data.rootValueHandler = handler;
        data.rootValuePlaceholder = "";
        data.rootValueDescription = "";
    }

    public static void setRootValueHandler(InlineParserData data,
                                           ValueHandler handler,
                                           String valuePlaceholder,
                                           String description) {
        if (handler == null) {
            throw new IllegalArgumentException("kcli root value handler must not be empty");
        }
        data.rootValueHandler = handler;
        data.rootValuePlaceholder = Normalization.normalizeHelpPlaceholderOrThrow(valuePlaceholder);
        data.rootValueDescription = Normalization.normalizeDescriptionOrThrow(description);
    }

    public static void setInlineHandler(InlineParserData data,
                                        String option,
                                        FlagHandler handler,
                                        String description) {
        String command = Normalization.normalizeInlineHandlerOptionOrThrow(option, data.rootName);
        data.commands.put(command, makeFlagBinding(handler, description));
    }

    public static void setInlineHandler(InlineParserData data,
                                        String option,
                                        ValueHandler handler,
                                        String description) {
        String command = Normalization.normalizeInlineHandlerOptionOrThrow(option, data.rootName);
        data.commands.put(command, makeValueBinding(handler, description, ValueArity.REQUIRED));
    }

    public static void setInlineOptionalValueHandler(InlineParserData data,
                                                     String option,
                                                     ValueHandler handler,
                                                     String description) {
        String command = Normalization.normalizeInlineHandlerOptionOrThrow(option, data.rootName);
        data.commands.put(command, makeValueBinding(handler, description, ValueArity.OPTIONAL));
    }

    public static void setAlias(ParserData data, String alias, String target) {
        setAlias(data, alias, target, new String[0]);
    }

    public static void setAlias(ParserData data,
                                String alias,
                                String target,
                                String... presetTokens) {
        String normalizedAlias = Normalization.normalizeAliasOrThrow(alias);
        String normalizedTarget = Normalization.normalizeAliasTargetOptionOrThrow(target);
        List<String> normalizedPresetTokens = new ArrayList<>();
        for (String token : presetTokens) {
            normalizedPresetTokens.add(token);
        }
        AliasBinding binding = new AliasBinding(normalizedAlias, normalizedTarget, normalizedPresetTokens);
        data.aliases.put(normalizedAlias, binding);
    }

    public static void setPrimaryHandler(ParserData data,
                                         String option,
                                         FlagHandler handler,
                                         String description) {
        String command = Normalization.normalizePrimaryHandlerOptionOrThrow(option);
        data.commands.put(command, makeFlagBinding(handler, description));
    }

    public static void setPrimaryHandler(ParserData data,
                                         String option,
                                         ValueHandler handler,
                                         String description) {
        String command = Normalization.normalizePrimaryHandlerOptionOrThrow(option);
        data.commands.put(command, makeValueBinding(handler, description, ValueArity.REQUIRED));
    }

    public static void setPrimaryOptionalValueHandler(ParserData data,
                                                      String option,
                                                      ValueHandler handler,
                                                      String description) {
        String command = Normalization.normalizePrimaryHandlerOptionOrThrow(option);
        data.commands.put(command, makeValueBinding(handler, description, ValueArity.OPTIONAL));
    }

    public static void setPositionalHandler(ParserData data, PositionalHandler handler) {
        if (handler == null) {
            throw new IllegalArgumentException("kcli positional handler must not be empty");
        }
        data.positionalHandler = handler;
    }

    public static void addInlineParser(ParserData data, InlineParserData parser) {
        if (data.inlineParsers.containsKey(parser.rootName)) {
            throw new IllegalArgumentException(
                "kcli inline parser root '--" + parser.rootName + "' is already registered");
        }
        data.inlineParsers.put(parser.rootName, parser.copy());
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
