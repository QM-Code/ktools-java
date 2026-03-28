package kcli.internal;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;

import kcli.FlagHandler;
import kcli.PositionalHandler;
import kcli.ValueHandler;

enum ValueArity {
    REQUIRED,
    OPTIONAL,
}

final class CommandBinding {
    final boolean expectsValue;
    final FlagHandler flagHandler;
    final ValueHandler valueHandler;
    final ValueArity valueArity;
    final String description;

    private CommandBinding(boolean expectsValue,
                           FlagHandler flagHandler,
                           ValueHandler valueHandler,
                           ValueArity valueArity,
                           String description) {
        this.expectsValue = expectsValue;
        this.flagHandler = flagHandler;
        this.valueHandler = valueHandler;
        this.valueArity = valueArity;
        this.description = description;
    }

    static CommandBinding flag(FlagHandler handler, String description) {
        return new CommandBinding(false, handler, null, ValueArity.REQUIRED, description);
    }

    static CommandBinding value(ValueHandler handler, String description, ValueArity arity) {
        return new CommandBinding(true, null, handler, arity, description);
    }

    CommandBinding copy() {
        return this;
    }
}

final class AliasBinding {
    final String alias;
    final String targetToken;
    final List<String> presetTokens;

    AliasBinding(String alias, String targetToken, List<String> presetTokens) {
        this.alias = alias;
        this.targetToken = targetToken;
        this.presetTokens = List.copyOf(presetTokens);
    }

    AliasBinding copy() {
        return this;
    }
}

final class MutableParseOutcome {
    boolean ok = true;
    String errorOption = "";
    String errorMessage = "";

    void reportError(String option, String message) {
        if (!ok) {
            return;
        }

        ok = false;
        errorOption = option == null ? "" : option;
        errorMessage = message == null ? "" : message;
    }
}

final class CollectedValues {
    boolean hasValue;
    final List<String> parts = new ArrayList<>();
    int lastIndex = -1;
}

enum InvocationKind {
    FLAG,
    VALUE,
    POSITIONAL,
    PRINT_HELP,
}

final class Invocation {
    InvocationKind kind = InvocationKind.FLAG;
    String root = "";
    String option = "";
    String command = "";
    final List<String> valueTokens = new ArrayList<>();
    FlagHandler flagHandler;
    ValueHandler valueHandler;
    PositionalHandler positionalHandler;
    final List<HelpRow> helpRows = new ArrayList<>();
}

record HelpRow(String lhs, String rhs) {
}

enum InlineTokenKind {
    NONE,
    BARE_ROOT,
    DASH_OPTION,
}

final class InlineTokenMatch {
    InlineTokenKind kind = InlineTokenKind.NONE;
    InlineParserData parser;
    String suffix = "";
}
