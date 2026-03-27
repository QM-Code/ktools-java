package kcli.internal;

import java.util.Objects;

import kcli.CliError;

final class Normalization {
    private Normalization() {
    }

    static void reportCliErrorAndExit(String message) {
        if (System.console() != null) {
            System.err.printf("[\u001b[31merror\u001b[0m] [\u001b[94mcli\u001b[0m] %s%n", message);
        } else {
            System.err.printf("[error] [cli] %s%n", message);
        }
        System.err.flush();
        System.exit(2);
    }

    static boolean startsWith(String value, String prefix) {
        return value.startsWith(prefix);
    }

    static String trimWhitespace(String value) {
        return Objects.toString(value, "").trim();
    }

    static boolean containsWhitespace(String value) {
        for (int i = 0; i < value.length(); ++i) {
            if (Character.isWhitespace(value.charAt(i))) {
                return true;
            }
        }
        return false;
    }

    static String normalizeRootNameOrThrow(String rawRoot) {
        String root = trimWhitespace(rawRoot);
        if (root.isEmpty()) {
            throw new IllegalArgumentException("kcli root must not be empty");
        }
        if (root.charAt(0) == '-') {
            throw new IllegalArgumentException("kcli root must not begin with '-'");
        }
        if (containsWhitespace(root)) {
            throw new IllegalArgumentException("kcli root is invalid");
        }
        return root;
    }

    static String normalizeInlineRootOptionOrThrow(String rawRoot) {
        String root = trimWhitespace(rawRoot);
        if (root.isEmpty()) {
            throw new IllegalArgumentException("kcli root must not be empty");
        }
        if (startsWith(root, "--")) {
            root = root.substring(2);
        } else if (root.charAt(0) == '-') {
            throw new IllegalArgumentException("kcli root must use '--root' or 'root'");
        }
        return normalizeRootNameOrThrow(root);
    }

    static String normalizeInlineHandlerOptionOrThrow(String rawOption,
                                                      String rootName) {
        String option = trimWhitespace(rawOption);
        if (option.isEmpty()) {
            throw new IllegalArgumentException("kcli inline handler option must not be empty");
        }
        if (startsWith(option, "--")) {
            String fullPrefix = "--" + rootName + "-";
            if (!startsWith(option, fullPrefix)) {
                throw new IllegalArgumentException(
                    "kcli inline handler option must use '-name' or '" + fullPrefix + "name'");
            }
            option = option.substring(fullPrefix.length());
        } else if (option.charAt(0) == '-') {
            option = option.substring(1);
        } else {
            throw new IllegalArgumentException(
                "kcli inline handler option must use '-name' or '--" + rootName + "-name'");
        }
        if (option.isEmpty()) {
            throw new IllegalArgumentException("kcli command must not be empty");
        }
        if (option.charAt(0) == '-') {
            throw new IllegalArgumentException("kcli command must not start with '-'");
        }
        if (containsWhitespace(option)) {
            throw new IllegalArgumentException("kcli command must not contain whitespace");
        }
        return option;
    }

    static String normalizePrimaryHandlerOptionOrThrow(String rawOption) {
        String option = trimWhitespace(rawOption);
        if (option.isEmpty()) {
            throw new IllegalArgumentException("kcli end-user handler option must not be empty");
        }
        if (startsWith(option, "--")) {
            option = option.substring(2);
        } else if (option.charAt(0) == '-') {
            throw new IllegalArgumentException("kcli end-user handler option must use '--name' or 'name'");
        }
        if (option.isEmpty()) {
            throw new IllegalArgumentException("kcli command must not be empty");
        }
        if (option.charAt(0) == '-') {
            throw new IllegalArgumentException("kcli command must not start with '-'");
        }
        if (containsWhitespace(option)) {
            throw new IllegalArgumentException("kcli command must not contain whitespace");
        }
        return option;
    }

    static String normalizeAliasOrThrow(String rawAlias) {
        String alias = trimWhitespace(rawAlias);
        if (alias.length() < 2 ||
            alias.charAt(0) != '-' ||
            startsWith(alias, "--") ||
            containsWhitespace(alias)) {
            throw new IllegalArgumentException("kcli alias must use single-dash form, e.g. '-v'");
        }
        return alias;
    }

    static String normalizeAliasTargetOptionOrThrow(String rawTarget) {
        String target = trimWhitespace(rawTarget);
        if (target.length() < 3 ||
            !startsWith(target, "--") ||
            containsWhitespace(target)) {
            throw new IllegalArgumentException(
                "kcli alias target must use double-dash form, e.g. '--verbose'");
        }
        if (target.charAt(2) == '-') {
            throw new IllegalArgumentException(
                "kcli alias target must use double-dash form, e.g. '--verbose'");
        }
        return target;
    }

    static String normalizeHelpPlaceholderOrThrow(String rawPlaceholder) {
        String placeholder = trimWhitespace(rawPlaceholder);
        if (placeholder.isEmpty()) {
            throw new IllegalArgumentException("kcli help placeholder must not be empty");
        }
        return placeholder;
    }

    static String normalizeDescriptionOrThrow(String rawDescription) {
        String description = trimWhitespace(rawDescription);
        if (description.isEmpty()) {
            throw new IllegalArgumentException("kcli command description must not be empty");
        }
        return description;
    }

    static void throwCliError(MutableParseOutcome result) {
        if (result.ok) {
            throw new IllegalStateException(
                "kcli internal error: throwCliError called without a failure");
        }
        throw new CliError(result.errorOption, result.errorMessage);
    }
}
