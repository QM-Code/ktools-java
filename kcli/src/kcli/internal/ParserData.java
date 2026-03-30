package kcli.internal;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;

import kcli.FlagHandler;
import kcli.PositionalHandler;
import kcli.ValueHandler;

public final class ParserData {
    PositionalHandler positionalHandler;
    final LinkedHashMap<String, AliasBinding> aliases = new LinkedHashMap<>();
    final LinkedHashMap<String, CommandBinding> commands = new LinkedHashMap<>();
    final LinkedHashMap<String, InlineParserData> inlineParsers = new LinkedHashMap<>();

    public ParserData() {
    }

    public void setAlias(String alias, String target) {
        setAlias(alias, target, new String[0]);
    }

    public void setAlias(String alias, String target, String... presetTokens) {
        String normalizedAlias = Normalization.normalizeAliasOrThrow(alias);
        String normalizedTarget = Normalization.normalizeAliasTargetOptionOrThrow(target);
        List<String> normalizedPresetTokens = new ArrayList<>();
        for (String token : presetTokens) {
            normalizedPresetTokens.add(token);
        }
        aliases.put(normalizedAlias, new AliasBinding(normalizedAlias, normalizedTarget, normalizedPresetTokens));
    }

    public void setPrimaryHandler(String option, FlagHandler handler, String description) {
        String command = Normalization.normalizePrimaryHandlerOptionOrThrow(option);
        commands.put(command, makeFlagBinding(handler, description));
    }

    public void setPrimaryHandler(String option, ValueHandler handler, String description) {
        String command = Normalization.normalizePrimaryHandlerOptionOrThrow(option);
        commands.put(command, makeValueBinding(handler, description, ValueArity.REQUIRED));
    }

    public void setPrimaryOptionalValueHandler(String option,
                                               ValueHandler handler,
                                               String description) {
        String command = Normalization.normalizePrimaryHandlerOptionOrThrow(option);
        commands.put(command, makeValueBinding(handler, description, ValueArity.OPTIONAL));
    }

    public void setPositionalHandler(PositionalHandler handler) {
        if (handler == null) {
            throw new IllegalArgumentException("kcli positional handler must not be empty");
        }
        positionalHandler = handler;
    }

    public void addInlineParser(InlineParserData parser) {
        if (inlineParsers.containsKey(parser.rootName)) {
            throw new IllegalArgumentException(
                "kcli inline parser root '--" + parser.rootName + "' is already registered");
        }
        inlineParsers.put(parser.rootName, parser.copy());
    }

    public void parseOrExit(int argc, String[] argv) {
        try {
            parse(argc, argv);
        } catch (kcli.CliError ex) {
            Normalization.reportCliErrorAndExit(ex.getMessage());
        }
    }

    public void parse(int argc, String[] argv) {
        new ParseSession(this, argc, argv).run();
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
