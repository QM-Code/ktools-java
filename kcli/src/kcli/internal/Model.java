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
    boolean expectsValue;
    FlagHandler flagHandler;
    ValueHandler valueHandler;
    ValueArity valueArity = ValueArity.REQUIRED;
    String description = "";

    CommandBinding copy() {
        CommandBinding copy = new CommandBinding();
        copy.expectsValue = expectsValue;
        copy.flagHandler = flagHandler;
        copy.valueHandler = valueHandler;
        copy.valueArity = valueArity;
        copy.description = description;
        return copy;
    }
}

final class AliasBinding {
    String alias = "";
    String targetToken = "";
    List<String> presetTokens = new ArrayList<>();

    AliasBinding copy() {
        AliasBinding copy = new AliasBinding();
        copy.alias = alias;
        copy.targetToken = targetToken;
        copy.presetTokens = new ArrayList<>(presetTokens);
        return copy;
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
