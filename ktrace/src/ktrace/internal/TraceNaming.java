package ktrace.internal;

import java.util.Arrays;
import java.util.List;
import java.util.Objects;
import java.util.regex.Pattern;

import ktrace.TraceColors;

final class TraceNaming {
    private static final Pattern IDENTIFIER = Pattern.compile("[A-Za-z0-9_-]+");
    private static final List<String> COLOR_NAMES = List.of(
        "Black",
        "Red",
        "Green",
        "Yellow",
        "Blue",
        "Magenta",
        "Cyan",
        "White",
        "BrightBlack",
        "BrightRed",
        "BrightGreen",
        "BrightYellow",
        "BrightBlue",
        "BrightMagenta",
        "BrightCyan",
        "BrightWhite",
        "DeepSkyBlue1",
        "Gold3",
        "MediumSpringGreen",
        "Orange3",
        "MediumOrchid1",
        "LightSkyBlue1",
        "LightSalmon1"
    );

    private TraceNaming() {
    }

    static int color(String colorName) {
        String token = trimWhitespace(colorName);
        if (token.isEmpty()) {
            throw new IllegalArgumentException("trace color name must not be empty");
        }
        if (token.equals("Default") || token.equals("default")) {
            return TraceColors.DEFAULT;
        }

        for (int index = 0; index < COLOR_NAMES.size(); ++index) {
            if (COLOR_NAMES.get(index).equals(token)) {
                return index;
            }
        }
        throw new IllegalArgumentException("unknown trace color '" + token + "'");
    }

    static List<String> colorNames() {
        return COLOR_NAMES;
    }

    static String trimWhitespace(String value) {
        return Objects.toString(value, "").trim();
    }

    static String normalizeNamespace(String traceNamespace) {
        String token = trimWhitespace(traceNamespace);
        if (!isSelectorIdentifier(token)) {
            throw new IllegalArgumentException("invalid trace namespace '" + token + "'");
        }
        return token;
    }

    static String normalizeChannel(String channel) {
        String token = trimWhitespace(channel);
        if (!isValidChannelPath(token)) {
            throw new IllegalArgumentException("invalid trace channel '" + token + "'");
        }
        return token;
    }

    static String makeQualifiedChannelKey(String traceNamespace, String channel) {
        String namespaceName = trimWhitespace(traceNamespace);
        String channelName = trimWhitespace(channel);
        if (namespaceName.isEmpty() || channelName.isEmpty()) {
            return "";
        }
        return namespaceName + "." + channelName;
    }

    static boolean isSelectorIdentifier(String token) {
        String normalized = trimWhitespace(token);
        return !normalized.isEmpty() && IDENTIFIER.matcher(normalized).matches();
    }

    static boolean isValidChannelPath(String channel) {
        List<String> parts = splitChannelPath(channel);
        if (parts.isEmpty() || parts.size() > 3) {
            return false;
        }
        for (String part : parts) {
            if (!isSelectorIdentifier(part)) {
                return false;
            }
        }
        return true;
    }

    static List<String> splitChannelPath(String channel) {
        String token = trimWhitespace(channel);
        if (token.isEmpty()) {
            return List.of();
        }
        return Arrays.asList(token.split("\\."));
    }

    static String simplifyFileName(String fileName, String className) {
        String candidate = trimWhitespace(fileName);
        if (!candidate.isEmpty()) {
            int dot = candidate.lastIndexOf('.');
            return dot > 0 ? candidate.substring(0, dot) : candidate;
        }

        int separator = className.lastIndexOf('.');
        String simple = separator >= 0 ? className.substring(separator + 1) : className;
        int dollar = simple.indexOf('$');
        return dollar >= 0 ? simple.substring(0, dollar) : simple;
    }
}
