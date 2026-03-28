package ktrace.internal;

import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

final class TraceSelectors {
    private TraceSelectors() {
    }

    static TraceInternals.ExactChannelResolution resolveExactChannel(TraceInternals.LoggerData loggerData,
                                                                     String qualifiedChannel,
                                                                     String localNamespace) {
        String qualified = TraceNaming.trimWhitespace(qualifiedChannel);
        int dot = qualified.indexOf('.');
        if (dot < 0) {
            throw new IllegalArgumentException(
                "invalid channel selector '" + qualified +
                "' (expected namespace.channel or .channel; use .channel for local namespace)");
        }

        String traceNamespace = dot == 0 ? TraceNaming.trimWhitespace(localNamespace) : qualified.substring(0, dot);
        String channel = qualified.substring(dot + 1);
        if (!TraceNaming.isSelectorIdentifier(traceNamespace)) {
            throw new IllegalArgumentException("invalid trace namespace '" + traceNamespace + "'");
        }
        if (!TraceNaming.isValidChannelPath(channel)) {
            throw new IllegalArgumentException("invalid trace channel '" + channel + "'");
        }

        String key = TraceNaming.makeQualifiedChannelKey(traceNamespace, channel);
        return new TraceInternals.ExactChannelResolution(
            key,
            traceNamespace,
            channel,
            TraceRegistry.isRegisteredTraceChannel(loggerData, traceNamespace, channel));
    }

    static TraceInternals.SelectorResolution resolveSelectorExpression(TraceInternals.LoggerData loggerData,
                                                                       String selectorsCsv,
                                                                       String localNamespace) {
        String selectorText = TraceNaming.trimWhitespace(selectorsCsv);
        if (selectorText.isEmpty()) {
            throw new IllegalArgumentException("EnableChannels requires one or more selectors");
        }

        List<String> invalidTokens = new ArrayList<>();
        List<TraceInternals.Selector> selectors = parseSelectorList(selectorText, localNamespace, invalidTokens);
        if (!invalidTokens.isEmpty()) {
            StringBuilder builder = new StringBuilder();
            builder.append("Invalid trace selector");
            if (invalidTokens.size() > 1) {
                builder.append('s');
            }
            builder.append(": ");
            for (int index = 0; index < invalidTokens.size(); ++index) {
                if (index > 0) {
                    builder.append(", ");
                }
                builder.append(formatInvalidSelector(invalidTokens.get(index)));
            }
            throw new IllegalArgumentException(builder.toString());
        }

        return resolveSelectorsToChannelKeys(loggerData, selectors);
    }

    private static List<TraceInternals.Selector> parseSelectorList(String value,
                                                                   String localNamespace,
                                                                   List<String> invalidTokens) {
        List<TraceInternals.Selector> selectors = new ArrayList<>();
        List<String> selectorTokens = new ArrayList<>();
        String splitError = splitByTopLevelCommas(value, selectorTokens);
        if (splitError != null) {
            invalidTokens.add(splitError);
            return selectors;
        }

        Set<String> invalidSeen = new LinkedHashSet<>();
        for (String token : selectorTokens) {
            String name = TraceNaming.trimWhitespace(token);
            if (name.isEmpty()) {
                if (invalidSeen.add("<empty>")) {
                    invalidTokens.add("<empty>");
                }
                continue;
            }

            List<String> expandedTokens = new ArrayList<>();
            String expandError = expandBraceExpression(name, expandedTokens);
            if (expandError != null) {
                String reason = name + " (" + expandError + ")";
                if (invalidSeen.add(reason)) {
                    invalidTokens.add(reason);
                }
                continue;
            }

            for (String expanded : expandedTokens) {
                try {
                    selectors.add(parseSelector(expanded, localNamespace));
                } catch (IllegalArgumentException ex) {
                    String reason = expanded + " (" + ex.getMessage() + ")";
                    if (invalidSeen.add(reason)) {
                        invalidTokens.add(reason);
                    }
                }
            }
        }

        return selectors;
    }

    private static TraceInternals.SelectorResolution resolveSelectorsToChannelKeys(TraceInternals.LoggerData loggerData,
                                                                                   List<TraceInternals.Selector> selectors) {
        List<String> channelKeys = new ArrayList<>();
        List<String> unmatchedSelectors = new ArrayList<>();
        Set<String> seenKeys = new LinkedHashSet<>();
        boolean[] matched = new boolean[selectors.size()];

        synchronized (loggerData.registryLock) {
            for (Map.Entry<String, List<String>> entry : loggerData.channelsByNamespace.entrySet()) {
                String traceNamespace = entry.getKey();
                for (String channel : entry.getValue()) {
                    for (int index = 0; index < selectors.size(); ++index) {
                        if (!matchesSelector(selectors.get(index), traceNamespace, channel)) {
                            continue;
                        }
                        matched[index] = true;
                        String key = TraceNaming.makeQualifiedChannelKey(traceNamespace, channel);
                        if (seenKeys.add(key)) {
                            channelKeys.add(key);
                        }
                    }
                }
            }
        }

        Set<String> unmatchedSeen = new LinkedHashSet<>();
        for (int index = 0; index < selectors.size(); ++index) {
            if (matched[index]) {
                continue;
            }
            String selectorText = formatSelector(selectors.get(index));
            if (unmatchedSeen.add(selectorText)) {
                unmatchedSelectors.add(selectorText);
            }
        }

        return new TraceInternals.SelectorResolution(channelKeys, unmatchedSelectors);
    }

    private static TraceInternals.Selector parseSelector(String rawToken, String localNamespace) {
        int dot = rawToken.indexOf('.');
        if (dot < 0) {
            throw new IllegalArgumentException("did you mean '.*'?");
        }

        String namespaceToken = rawToken.substring(0, dot);
        String channelPattern = rawToken.substring(dot + 1);
        boolean anyNamespace = false;
        String traceNamespace = "";
        if (namespaceToken.equals("*")) {
            anyNamespace = true;
        } else if (namespaceToken.isEmpty()) {
            traceNamespace = TraceNaming.trimWhitespace(localNamespace);
            if (!TraceNaming.isSelectorIdentifier(traceNamespace)) {
                throw new IllegalArgumentException("missing namespace");
            }
        } else if (TraceNaming.isSelectorIdentifier(namespaceToken)) {
            traceNamespace = namespaceToken;
        } else {
            throw new IllegalArgumentException("invalid namespace '" + namespaceToken + "'");
        }

        List<String> tokens = TraceNaming.splitChannelPath(channelPattern);
        if (tokens.isEmpty()) {
            throw new IllegalArgumentException("missing channel expression");
        }
        if (tokens.size() > 3) {
            throw new IllegalArgumentException("channel depth exceeds 3");
        }
        for (String token : tokens) {
            if (!token.equals("*") && !TraceNaming.isSelectorIdentifier(token)) {
                throw new IllegalArgumentException("invalid channel token '" + token + "'");
            }
        }
        boolean includeTopLevel = tokens.size() == 2 && tokens.get(0).equals("*") && tokens.get(1).equals("*");
        return new TraceInternals.Selector(anyNamespace, traceNamespace, List.copyOf(tokens), includeTopLevel);
    }

    private static boolean matchesSelector(TraceInternals.Selector selector,
                                           String traceNamespace,
                                           String channel) {
        if (!selector.anyNamespace() && !traceNamespace.equals(selector.traceNamespace())) {
            return false;
        }
        List<String> channelParts = TraceNaming.splitChannelPath(channel);
        if (channelParts.isEmpty()) {
            return false;
        }

        List<String> pattern = selector.channelTokens();
        if (pattern.size() == 1) {
            return channelParts.size() == 1 && matchesSelectorSegment(pattern.get(0), channelParts.get(0));
        }

        if (pattern.size() == 2) {
            if (channelParts.size() == 1 && selector.includeTopLevel()) {
                return true;
            }
            return channelParts.size() == 2 &&
                matchesSelectorSegment(pattern.get(0), channelParts.get(0)) &&
                matchesSelectorSegment(pattern.get(1), channelParts.get(1));
        }

        if (pattern.get(0).equals("*") && pattern.get(1).equals("*") && pattern.get(2).equals("*")) {
            return channelParts.size() >= 1 && channelParts.size() <= 3;
        }

        return channelParts.size() == 3 &&
            matchesSelectorSegment(pattern.get(0), channelParts.get(0)) &&
            matchesSelectorSegment(pattern.get(1), channelParts.get(1)) &&
            matchesSelectorSegment(pattern.get(2), channelParts.get(2));
    }

    private static boolean matchesSelectorSegment(String pattern, String value) {
        return pattern.equals("*") || pattern.equals(value);
    }

    private static String splitByTopLevelCommas(String value, List<String> parts) {
        int braceDepth = 0;
        int start = 0;
        for (int index = 0; index < value.length(); ++index) {
            char current = value.charAt(index);
            if (current == '{') {
                braceDepth += 1;
                continue;
            }
            if (current == '}') {
                if (braceDepth == 0) {
                    return "unmatched '}'";
                }
                braceDepth -= 1;
                continue;
            }
            if (current == ',' && braceDepth == 0) {
                parts.add(TraceNaming.trimWhitespace(value.substring(start, index)));
                start = index + 1;
            }
        }
        if (braceDepth != 0) {
            return "unmatched '{'";
        }
        parts.add(TraceNaming.trimWhitespace(value.substring(start)));
        return null;
    }

    private static String expandBraceExpression(String value, List<String> expanded) {
        int open = value.indexOf('{');
        if (open < 0) {
            expanded.add(value);
            return null;
        }

        int depth = 0;
        int close = -1;
        for (int index = open; index < value.length(); ++index) {
            char current = value.charAt(index);
            if (current == '{') {
                depth += 1;
            } else if (current == '}') {
                depth -= 1;
                if (depth == 0) {
                    close = index;
                    break;
                }
            }
        }
        if (close < 0) {
            return "unmatched '{'";
        }

        String prefix = value.substring(0, open);
        String suffix = value.substring(close + 1);
        String inside = value.substring(open + 1, close);
        List<String> alternatives = new ArrayList<>();
        String splitError = splitByTopLevelCommas(inside, alternatives);
        if (splitError != null) {
            return splitError;
        }
        if (alternatives.isEmpty()) {
            return "empty brace group";
        }
        for (String alternative : alternatives) {
            if (alternative.isEmpty()) {
                return "empty brace alternative";
            }
            String nestedError = expandBraceExpression(prefix + alternative + suffix, expanded);
            if (nestedError != null) {
                return nestedError;
            }
        }
        return null;
    }

    private static String formatInvalidSelector(String token) {
        int reasonPos = token.indexOf(" (");
        if (reasonPos >= 0) {
            return "'" + token.substring(0, reasonPos) + "'" + token.substring(reasonPos);
        }
        return "'" + token + "'";
    }

    private static String formatSelector(TraceInternals.Selector selector) {
        StringBuilder text = new StringBuilder();
        text.append(selector.anyNamespace() ? "*" : selector.traceNamespace());
        text.append('.');
        for (int index = 0; index < selector.channelTokens().size(); ++index) {
            if (index > 0) {
                text.append('.');
            }
            text.append(selector.channelTokens().get(index));
        }
        return text.toString();
    }
}
