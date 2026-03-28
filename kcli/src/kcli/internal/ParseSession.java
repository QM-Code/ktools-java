package kcli.internal;

import java.util.ArrayList;
import java.util.List;

import kcli.HandlerContext;

final class ParseSession {
    private final ParserData data;
    private final int argc;
    private final String[] argv;
    private final MutableParseOutcome result = new MutableParseOutcome();
    private final List<Invocation> invocations = new ArrayList<>();
    private boolean[] consumed;
    private List<String> tokens = List.of();

    ParseSession(ParserData data, int argc, String[] argv) {
        this.data = data;
        this.argc = argc;
        this.argv = argv;
    }

    void run() {
        if (argc > 0 && argv == null) {
            result.reportError("", "kcli received invalid argv (argc > 0 but argv is null)");
            Normalization.throwCliError(result);
        }

        if (argc <= 0 || argv == null) {
            return;
        }

        if (argv.length < argc) {
            result.reportError("", "kcli received invalid argv (argv shorter than argc)");
            Normalization.throwCliError(result);
        }

        consumed = new boolean[argc];
        tokens = buildParseTokens();

        int index = 1;
        while (index < argc) {
            if (consumed[index]) {
                index += 1;
                continue;
            }

            String arg = tokens.get(index);
            if (arg.isEmpty()) {
                index += 1;
                continue;
            }

            AliasBinding aliasBinding = null;
            String effectiveArg = arg;
            if (arg.charAt(0) == '-' && !Normalization.startsWith(arg, "--")) {
                aliasBinding = data.aliases.get(arg);
                if (aliasBinding != null) {
                    effectiveArg = aliasBinding.targetToken;
                }
            }

            if (effectiveArg.charAt(0) != '-') {
                index += 1;
                continue;
            }

            if (effectiveArg.equals("--")) {
                index += 1;
                continue;
            }

            if (Normalization.startsWith(effectiveArg, "--")) {
                index = scheduleOption(index, effectiveArg, aliasBinding);
            }

            if (!result.ok) {
                break;
            }

            index += 1;
        }

        if (result.ok) {
            schedulePositionals();
        }

        if (result.ok) {
            validateNoUnknownOptionsRemain();
        }

        if (result.ok) {
            executeInvocations();
        }

        if (!result.ok) {
            Normalization.throwCliError(result);
        }
    }

    private int scheduleOption(int index, String effectiveArg, AliasBinding aliasBinding) {
        InlineTokenMatch inlineMatch = matchInlineToken(effectiveArg);
        switch (inlineMatch.kind) {
        case BARE_ROOT:
            return scheduleBareInlineRoot(index, effectiveArg, aliasBinding, inlineMatch);
        case DASH_OPTION:
            return scheduleInlineOption(index, effectiveArg, aliasBinding, inlineMatch);
        case NONE:
            return schedulePrimaryOption(index, effectiveArg, aliasBinding);
        }
        return index;
    }

    private int scheduleBareInlineRoot(int index,
                                       String effectiveArg,
                                       AliasBinding aliasBinding,
                                       InlineTokenMatch inlineMatch) {
        consumeIndex(index);
        CollectedValues collected = collectValueTokens(index, false);
        if (!collected.hasValue && !hasAliasPresetTokens(aliasBinding)) {
            Invocation help = new Invocation();
            help.kind = InvocationKind.PRINT_HELP;
            help.root = inlineMatch.parser.rootName;
            help.helpRows.addAll(buildHelpRows(inlineMatch.parser));
            invocations.add(help);
            return index;
        }

        if (inlineMatch.parser.rootValueHandler == null) {
            result.reportError(effectiveArg, "unknown value for option '" + effectiveArg + "'");
            return index;
        }

        Invocation invocation = new Invocation();
        invocation.kind = InvocationKind.VALUE;
        invocation.root = inlineMatch.parser.rootName;
        invocation.option = effectiveArg;
        invocation.valueHandler = inlineMatch.parser.rootValueHandler;
        invocation.valueTokens.addAll(buildEffectiveValueTokens(aliasBinding, collected.parts));
        invocations.add(invocation);
        return collected.hasValue ? collected.lastIndex : index;
    }

    private int scheduleInlineOption(int index,
                                     String effectiveArg,
                                     AliasBinding aliasBinding,
                                     InlineTokenMatch inlineMatch) {
        if (inlineMatch.suffix.isEmpty()) {
            return index;
        }

        CommandBinding binding = inlineMatch.parser.commands.get(inlineMatch.suffix);
        if (binding == null) {
            return index;
        }

        return scheduleInvocation(
            binding,
            aliasBinding,
            inlineMatch.parser.rootName,
            inlineMatch.suffix,
            effectiveArg,
            index);
    }

    private int schedulePrimaryOption(int index, String effectiveArg, AliasBinding aliasBinding) {
        String command = effectiveArg.substring(2);
        CommandBinding binding = data.commands.get(command);
        if (binding == null) {
            return index;
        }

        return scheduleInvocation(binding, aliasBinding, "", command, effectiveArg, index);
    }

    private boolean isCollectableFollowOnValueToken(String value) {
        return value.isEmpty() || value.charAt(0) != '-';
    }

    private String joinWithSpaces(List<String> parts) {
        return String.join(" ", parts);
    }

    private String formatOptionErrorMessage(String option, String message) {
        if (option == null || option.isEmpty()) {
            return message == null ? "" : message;
        }
        return "option '" + option + "': " + (message == null ? "" : message);
    }

    private CollectedValues collectValueTokens(int optionIndex, boolean allowOptionLikeFirstValue) {
        CollectedValues collected = new CollectedValues();
        collected.lastIndex = optionIndex;

        int firstValueIndex = optionIndex + 1;
        boolean hasNext = firstValueIndex >= 0 &&
            firstValueIndex < tokens.size() &&
            !consumed[firstValueIndex];
        if (!hasNext) {
            return collected;
        }

        String first = tokens.get(firstValueIndex);
        if (!allowOptionLikeFirstValue && !first.isEmpty() && first.charAt(0) == '-') {
            return collected;
        }

        collected.hasValue = true;
        collected.parts.add(first);
        consumed[firstValueIndex] = true;
        collected.lastIndex = firstValueIndex;

        if (allowOptionLikeFirstValue && !first.isEmpty() && first.charAt(0) == '-') {
            return collected;
        }

        for (int scan = firstValueIndex + 1; scan < tokens.size(); ++scan) {
            if (consumed[scan]) {
                continue;
            }

            String next = tokens.get(scan);
            if (!isCollectableFollowOnValueToken(next)) {
                break;
            }

            collected.parts.add(next);
            consumed[scan] = true;
            collected.lastIndex = scan;
        }

        return collected;
    }

    private void printHelp(Invocation invocation) {
        System.out.println();
        System.out.println("Available --" + invocation.root + "-* options:");

        int maxLhs = 0;
        for (HelpRow row : invocation.helpRows) {
            maxLhs = Math.max(maxLhs, row.lhs().length());
        }

        if (invocation.helpRows.isEmpty()) {
            System.out.println("  (no options registered)");
        } else {
            for (HelpRow row : invocation.helpRows) {
                StringBuilder line = new StringBuilder();
                line.append("  ").append(row.lhs());
                int padding = maxLhs > row.lhs().length() ? (maxLhs - row.lhs().length()) : 0;
                line.append(" ".repeat(padding + 2));
                line.append(row.rhs());
                System.out.println(line);
            }
        }

        System.out.println();
    }

    private void consumeIndex(int index) {
        if (index >= 0 && index < consumed.length && !consumed[index]) {
            consumed[index] = true;
        }
    }

    private boolean hasAliasPresetTokens(AliasBinding aliasBinding) {
        return aliasBinding != null && !aliasBinding.presetTokens.isEmpty();
    }

    private List<String> buildEffectiveValueTokens(AliasBinding aliasBinding, List<String> collectedParts) {
        if (!hasAliasPresetTokens(aliasBinding)) {
            return new ArrayList<>(collectedParts);
        }

        List<String> merged = new ArrayList<>(aliasBinding.presetTokens.size() + collectedParts.size());
        merged.addAll(aliasBinding.presetTokens);
        merged.addAll(collectedParts);
        return merged;
    }

    private List<HelpRow> buildHelpRows(InlineParserData parser) {
        String prefix = "--" + parser.rootName + "-";
        List<HelpRow> rows = new ArrayList<>();

        if (parser.rootValueHandler != null && !parser.rootValueDescription.isEmpty()) {
            String lhs = "--" + parser.rootName;
            if (!parser.rootValuePlaceholder.isEmpty()) {
                lhs += " " + parser.rootValuePlaceholder;
            }
            rows.add(new HelpRow(lhs, parser.rootValueDescription));
        }

        for (var entry : parser.commands.entrySet()) {
            String lhs = prefix + entry.getKey();
            CommandBinding binding = entry.getValue();
            if (binding.expectsValue) {
                if (binding.valueArity == ValueArity.OPTIONAL) {
                    lhs += " [value]";
                } else if (binding.valueArity == ValueArity.REQUIRED) {
                    lhs += " <value>";
                }
            }
            rows.add(new HelpRow(lhs, binding.description));
        }

        return rows;
    }

    private InlineTokenMatch matchInlineToken(String arg) {
        for (InlineParserData parser : data.inlineParsers.values()) {
            String rootOption = "--" + parser.rootName;
            if (arg.equals(rootOption)) {
                InlineTokenMatch match = new InlineTokenMatch();
                match.kind = InlineTokenKind.BARE_ROOT;
                match.parser = parser;
                return match;
            }

            String rootDashPrefix = rootOption + "-";
            if (Normalization.startsWith(arg, rootDashPrefix)) {
                InlineTokenMatch match = new InlineTokenMatch();
                match.kind = InlineTokenKind.DASH_OPTION;
                match.parser = parser;
                match.suffix = arg.substring(rootDashPrefix.length());
                return match;
            }
        }

        return new InlineTokenMatch();
    }

    private int scheduleInvocation(CommandBinding binding,
                                   AliasBinding aliasBinding,
                                   String root,
                                   String command,
                                   String optionToken,
                                   int index) {
        consumeIndex(index);

        Invocation invocation = new Invocation();
        invocation.root = root;
        invocation.option = optionToken;
        invocation.command = command;

        if (!binding.expectsValue) {
            if (hasAliasPresetTokens(aliasBinding)) {
                result.reportError(
                    aliasBinding.alias,
                    "alias '" + aliasBinding.alias + "' presets values for option '" +
                        optionToken + "' which does not accept values");
                return index;
            }

            invocation.kind = InvocationKind.FLAG;
            invocation.flagHandler = binding.flagHandler;
            invocations.add(invocation);
            return index;
        }

        CollectedValues collected = collectValueTokens(index, binding.valueArity == ValueArity.REQUIRED);
        if (!collected.hasValue &&
            !hasAliasPresetTokens(aliasBinding) &&
            binding.valueArity == ValueArity.REQUIRED) {
            result.reportError(optionToken, "option '" + optionToken + "' requires a value");
            return index;
        }

        if (collected.hasValue) {
            index = collected.lastIndex;
        }

        invocation.kind = InvocationKind.VALUE;
        invocation.valueHandler = binding.valueHandler;
        invocation.valueTokens.addAll(buildEffectiveValueTokens(aliasBinding, collected.parts));
        invocations.add(invocation);
        return index;
    }

    private void schedulePositionals() {
        if (data.positionalHandler == null || tokens.size() <= 1) {
            return;
        }

        Invocation invocation = new Invocation();
        invocation.kind = InvocationKind.POSITIONAL;
        invocation.positionalHandler = data.positionalHandler;

        for (int index = 1; index < tokens.size(); ++index) {
            if (consumed[index]) {
                continue;
            }

            String token = tokens.get(index);
            if (token.isEmpty() || token.charAt(0) != '-') {
                consumed[index] = true;
                invocation.valueTokens.add(token);
            }
        }

        if (!invocation.valueTokens.isEmpty()) {
            invocations.add(invocation);
        }
    }

    private List<String> buildParseTokens() {
        List<String> built = new ArrayList<>(argc);
        for (int index = 0; index < argc; ++index) {
            built.add(argv[index] == null ? "" : argv[index]);
        }
        return built;
    }

    private void validateNoUnknownOptionsRemain() {
        for (int scan = 1; scan < argc; ++scan) {
            if (consumed[scan]) {
                continue;
            }

            String token = tokens.get(scan);
            if (!token.isEmpty() && token.charAt(0) == '-') {
                result.reportError(token, "unknown option " + token);
                break;
            }
        }
    }

    private void executeInvocations() {
        for (Invocation invocation : invocations) {
            if (!result.ok) {
                return;
            }

            if (invocation.kind == InvocationKind.PRINT_HELP) {
                printHelp(invocation);
                continue;
            }

            HandlerContext context = new HandlerContext(
                invocation.root,
                invocation.option,
                invocation.command,
                invocation.valueTokens);

            try {
                switch (invocation.kind) {
                case FLAG -> invocation.flagHandler.handle(context);
                case VALUE -> invocation.valueHandler.handle(context, joinWithSpaces(invocation.valueTokens));
                case POSITIONAL -> invocation.positionalHandler.handle(context);
                case PRINT_HELP -> {
                }
                }
            } catch (Exception ex) {
                result.reportError(
                    invocation.option,
                    formatOptionErrorMessage(invocation.option, ex.getMessage()));
            } catch (Throwable ex) {
                result.reportError(
                    invocation.option,
                    formatOptionErrorMessage(
                        invocation.option,
                        "unknown exception while handling option"));
            }
        }
    }
}
