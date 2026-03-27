package ktrace.internal;

import java.io.PrintStream;
import java.lang.ref.WeakReference;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.regex.Pattern;

import ktrace.OutputOptions;
import ktrace.TraceColors;

public final class TraceInternals {
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

    private TraceInternals() {
    }

    public enum LogSeverity {
        INFO("info"),
        WARNING("warning"),
        ERROR("error");

        private final String label;

        LogSeverity(String label) {
            this.label = label;
        }

        public String label() {
            return label;
        }
    }

    public record CallSite(String fileName, int lineNumber, String methodName) {
    }

    public record ChannelSpec(String name, int color) {
    }

    public record ExactChannelResolution(String key,
                                         String traceNamespace,
                                         String channel,
                                         boolean registered) {
    }

    public record Selector(boolean anyNamespace,
                           String traceNamespace,
                           List<String> channelTokens,
                           boolean includeTopLevel) {
    }

    public record SelectorResolution(List<String> channelKeys, List<String> unmatchedSelectors) {
    }

    public record TraceLoggerData(String traceNamespace,
                                  List<ChannelSpec> channels,
                                  ConcurrentHashMap<String, String> changedKeys,
                                  Object attachedLoggerLock,
                                  WeakReference<LoggerData>[] attachedLoggerRef) {
        @SuppressWarnings("unchecked")
        public TraceLoggerData(String traceNamespace) {
            this(traceNamespace,
                new ArrayList<>(),
                new ConcurrentHashMap<>(),
                new Object(),
                new WeakReference[] {new WeakReference<>(null)});
        }
    }

    public static final class LoggerData {
        private final Object registryLock = new Object();
        private final Object enabledLock = new Object();
        private final Object outputLock = new Object();
        private final AtomicBoolean hasEnabledChannels = new AtomicBoolean(false);
        private volatile boolean filenamesEnabled;
        private volatile boolean lineNumbersEnabled;
        private volatile boolean functionNamesEnabled;
        private volatile boolean timestampsEnabled;
        private final Set<String> namespaces = new LinkedHashSet<>();
        private final Map<String, List<String>> channelsByNamespace = new LinkedHashMap<>();
        private final Map<String, Map<String, Integer>> colorsByNamespace = new LinkedHashMap<>();
        private final Set<String> enabledChannelKeys = new LinkedHashSet<>();
        private final List<TraceLoggerData> attachedTraceLoggers = new ArrayList<>();
    }

    public static int color(String colorName) {
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

    public static List<String> colorNames() {
        return COLOR_NAMES;
    }

    public static String trimWhitespace(String value) {
        return Objects.toString(value, "").trim();
    }

    public static String normalizeNamespace(String traceNamespace) {
        String token = trimWhitespace(traceNamespace);
        if (!isSelectorIdentifier(token)) {
            throw new IllegalArgumentException("invalid trace namespace '" + token + "'");
        }
        return token;
    }

    public static String normalizeChannel(String channel) {
        String token = trimWhitespace(channel);
        if (!isValidChannelPath(token)) {
            throw new IllegalArgumentException("invalid trace channel '" + token + "'");
        }
        return token;
    }

    public static void addChannel(TraceLoggerData data, String channel, int color) {
        String traceNamespace = normalizeNamespace(data.traceNamespace());
        String channelName = normalizeChannel(channel);
        int parentSeparator = channelName.lastIndexOf('.');
        if (parentSeparator >= 0) {
            String parentChannel = channelName.substring(0, parentSeparator);
            if (findChannelSpec(data, parentChannel) == null) {
                throw new IllegalArgumentException(
                    "cannot add unparented trace channel '" + channelName + "' (missing parent '" + parentChannel + "')");
            }
        }

        ChannelSpec existing = findChannelSpec(data, channelName);
        if (existing != null) {
            int merged = mergeColor(existing.color(), color, traceNamespace, channelName);
            if (merged != existing.color()) {
                data.channels().set(data.channels().indexOf(existing), new ChannelSpec(existing.name(), merged));
            }
            return;
        }

        if (color != TraceColors.DEFAULT && (color < 0 || color > 255)) {
            throw new IllegalArgumentException("invalid trace color id '" + color + "'");
        }
        data.channels().add(new ChannelSpec(channelName, color));
    }

    public static void ensureTraceLoggerCanAttach(TraceLoggerData traceLogger, LoggerData loggerData) {
        synchronized (traceLogger.attachedLoggerLock()) {
            LoggerData attached = traceLogger.attachedLoggerRef()[0].get();
            if (attached != null && attached != loggerData) {
                throw new IllegalArgumentException("trace logger is already attached to another logger");
            }
        }
    }

    public static void attachTraceLogger(TraceLoggerData traceLogger, LoggerData loggerData) {
        synchronized (traceLogger.attachedLoggerLock()) {
            LoggerData attached = traceLogger.attachedLoggerRef()[0].get();
            if (attached != null && attached != loggerData) {
                throw new IllegalArgumentException("trace logger is already attached to another logger");
            }
            traceLogger.attachedLoggerRef()[0] = new WeakReference<>(loggerData);
        }
    }

    public static LoggerData attachedLogger(TraceLoggerData traceLogger) {
        synchronized (traceLogger.attachedLoggerLock()) {
            return traceLogger.attachedLoggerRef()[0].get();
        }
    }

    public static void mergeTraceLogger(LoggerData loggerData, TraceLoggerData traceLogger) {
        String traceNamespace = normalizeNamespace(traceLogger.traceNamespace());
        synchronized (loggerData.registryLock) {
            loggerData.namespaces.add(traceNamespace);
            List<String> registeredChannels =
                loggerData.channelsByNamespace.computeIfAbsent(traceNamespace, ignored -> new ArrayList<>());
            Map<String, Integer> registeredColors =
                loggerData.colorsByNamespace.computeIfAbsent(traceNamespace, ignored -> new LinkedHashMap<>());

            for (ChannelSpec channel : traceLogger.channels()) {
                String channelName = normalizeChannel(channel.name());
                int parentSeparator = channelName.lastIndexOf('.');
                if (parentSeparator >= 0) {
                    String parentChannel = channelName.substring(0, parentSeparator);
                    if (!registeredChannels.contains(parentChannel)) {
                        throw new IllegalArgumentException(
                            "cannot register unparented trace channel '" + channelName + "' (missing parent '" + parentChannel + "')");
                    }
                }

                if (!registeredChannels.contains(channelName)) {
                    registeredChannels.add(channelName);
                }

                int existingColor = registeredColors.getOrDefault(channelName, TraceColors.DEFAULT);
                int mergedColor = mergeColor(existingColor, channel.color(), traceNamespace, channelName);
                if (mergedColor != TraceColors.DEFAULT) {
                    registeredColors.put(channelName, mergedColor);
                }
            }
        }
    }

    public static void retainTraceLogger(LoggerData loggerData, TraceLoggerData traceLogger) {
        synchronized (loggerData.registryLock) {
            if (!loggerData.attachedTraceLoggers.contains(traceLogger)) {
                loggerData.attachedTraceLoggers.add(traceLogger);
            }
        }
    }

    public static OutputOptions getOutputOptions(LoggerData loggerData) {
        return new OutputOptions(
            loggerData.filenamesEnabled,
            loggerData.lineNumbersEnabled,
            loggerData.functionNamesEnabled,
            loggerData.timestampsEnabled);
    }

    public static void setOutputOptions(LoggerData loggerData, OutputOptions options) {
        loggerData.filenamesEnabled = options.filenames();
        loggerData.lineNumbersEnabled = options.filenames() && options.lineNumbers();
        loggerData.functionNamesEnabled = options.filenames() && options.functionNames();
        loggerData.timestampsEnabled = options.timestamps();
    }

    public static List<String> getNamespaces(LoggerData loggerData) {
        synchronized (loggerData.registryLock) {
            List<String> namespaces = new ArrayList<>(loggerData.namespaces);
            Collections.sort(namespaces);
            return namespaces;
        }
    }

    public static List<String> getChannels(LoggerData loggerData, String traceNamespace) {
        String namespaceName = normalizeNamespace(traceNamespace);
        synchronized (loggerData.registryLock) {
            List<String> channels = loggerData.channelsByNamespace.get(namespaceName);
            if (channels == null) {
                return List.of();
            }
            List<String> copy = new ArrayList<>(channels);
            Collections.sort(copy);
            return copy;
        }
    }

    public static boolean shouldTraceChannel(TraceLoggerData traceLogger, String channel) {
        try {
            LoggerData logger = attachedLogger(traceLogger);
            if (logger == null) {
                return false;
            }
            return shouldTraceChannel(logger, traceLogger.traceNamespace(), normalizeChannel(channel));
        } catch (RuntimeException ex) {
            return false;
        }
    }

    public static boolean shouldTraceQualifiedChannel(LoggerData loggerData,
                                                      String qualifiedChannel,
                                                      String localNamespace) {
        try {
            ExactChannelResolution resolution = resolveExactChannel(loggerData, qualifiedChannel, localNamespace);
            return shouldTraceChannel(loggerData, resolution.traceNamespace(), resolution.channel());
        } catch (RuntimeException ex) {
            return false;
        }
    }

    public static boolean shouldTraceChannel(LoggerData loggerData,
                                             String traceNamespace,
                                             String channel) {
        if (!isValidChannelPath(channel) || !loggerData.hasEnabledChannels.get()) {
            return false;
        }
        if (!isRegisteredTraceChannel(loggerData, traceNamespace, channel)) {
            return false;
        }
        String key = makeQualifiedChannelKey(traceNamespace, channel);
        synchronized (loggerData.enabledLock) {
            return loggerData.enabledChannelKeys.contains(key);
        }
    }

    public static boolean isRegisteredTraceChannel(LoggerData loggerData,
                                                   String traceNamespace,
                                                   String channel) {
        String namespaceName = trimWhitespace(traceNamespace);
        String channelName = trimWhitespace(channel);
        if (!isSelectorIdentifier(namespaceName) || !isValidChannelPath(channelName)) {
            return false;
        }

        synchronized (loggerData.registryLock) {
            List<String> channels = loggerData.channelsByNamespace.get(namespaceName);
            return channels != null && channels.contains(channelName);
        }
    }

    public static void enableChannelKeys(LoggerData loggerData, List<String> channelKeys) {
        synchronized (loggerData.enabledLock) {
            for (String key : channelKeys) {
                if (key != null && !key.isEmpty()) {
                    loggerData.enabledChannelKeys.add(key);
                }
            }
            loggerData.hasEnabledChannels.set(!loggerData.enabledChannelKeys.isEmpty());
        }
    }

    public static void disableChannelKeys(LoggerData loggerData, List<String> channelKeys) {
        synchronized (loggerData.enabledLock) {
            loggerData.enabledChannelKeys.removeAll(channelKeys);
            loggerData.hasEnabledChannels.set(!loggerData.enabledChannelKeys.isEmpty());
        }
    }

    public static ExactChannelResolution resolveExactChannel(LoggerData loggerData,
                                                             String qualifiedChannel,
                                                             String localNamespace) {
        String qualified = trimWhitespace(qualifiedChannel);
        int dot = qualified.indexOf('.');
        if (dot < 0) {
            throw new IllegalArgumentException(
                "invalid channel selector '" + qualified +
                "' (expected namespace.channel or .channel; use .channel for local namespace)");
        }

        String traceNamespace = dot == 0 ? trimWhitespace(localNamespace) : qualified.substring(0, dot);
        String channel = qualified.substring(dot + 1);
        if (!isSelectorIdentifier(traceNamespace)) {
            throw new IllegalArgumentException("invalid trace namespace '" + traceNamespace + "'");
        }
        if (!isValidChannelPath(channel)) {
            throw new IllegalArgumentException("invalid trace channel '" + channel + "'");
        }

        String key = makeQualifiedChannelKey(traceNamespace, channel);
        return new ExactChannelResolution(
            key,
            traceNamespace,
            channel,
            isRegisteredTraceChannel(loggerData, traceNamespace, channel));
    }

    public static SelectorResolution resolveSelectorExpression(LoggerData loggerData,
                                                               String selectorsCsv,
                                                               String localNamespace) {
        String selectorText = trimWhitespace(selectorsCsv);
        if (selectorText.isEmpty()) {
            throw new IllegalArgumentException("EnableChannels requires one or more selectors");
        }

        List<String> invalidTokens = new ArrayList<>();
        List<Selector> selectors = parseSelectorList(selectorText, localNamespace, invalidTokens);
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

    public static CallSite captureCallSite() {
        StackTraceElement[] frames = Thread.currentThread().getStackTrace();
        boolean sawPublicApi = false;
        for (StackTraceElement frame : frames) {
            String className = frame.getClassName();
            if (className.equals(Thread.class.getName())) {
                continue;
            }
            if (className.equals("ktrace.TraceLogger") || className.equals("ktrace.Logger") ||
                className.startsWith("ktrace.internal.")) {
                sawPublicApi = true;
                continue;
            }
            if (!sawPublicApi) {
                continue;
            }
            return new CallSite(
                simplifyFileName(frame.getFileName(), className),
                frame.getLineNumber(),
                frame.getMethodName());
        }
        return new CallSite("unknown", -1, "");
    }

    public static String makeTraceChangedSiteKey(String channel, CallSite callSite) {
        return callSite.fileName() + ":" + callSite.lineNumber() + ":" + callSite.methodName() + ":" + channel;
    }

    public static String formatArgument(Object value) {
        return String.valueOf(value);
    }

    public static String formatMessage(String formatText, Object... args) {
        String text = Objects.toString(formatText, "");
        List<String> formattedArgs = new ArrayList<>();
        if (args != null) {
            for (Object arg : args) {
                formattedArgs.add(formatArgument(arg));
            }
        }

        StringBuilder out = new StringBuilder(text.length());
        int argIndex = 0;
        for (int index = 0; index < text.length(); ++index) {
            char ch = text.charAt(index);
            if (ch == '{') {
                if (index + 1 >= text.length()) {
                    throw new IllegalArgumentException("unterminated '{' in trace format string");
                }
                char next = text.charAt(index + 1);
                if (next == '{') {
                    out.append('{');
                    index += 1;
                    continue;
                }
                if (next == '}') {
                    if (argIndex >= formattedArgs.size()) {
                        throw new IllegalArgumentException("not enough arguments for trace format string");
                    }
                    out.append(formattedArgs.get(argIndex++));
                    index += 1;
                    continue;
                }
                throw new IllegalArgumentException("unsupported trace format token");
            }
            if (ch == '}') {
                if (index + 1 < text.length() && text.charAt(index + 1) == '}') {
                    out.append('}');
                    index += 1;
                    continue;
                }
                throw new IllegalArgumentException("unmatched '}' in trace format string");
            }
            out.append(ch);
        }

        if (argIndex != formattedArgs.size()) {
            throw new IllegalArgumentException("too many arguments for trace format string");
        }
        return out.toString();
    }

    public static void emitTrace(LoggerData loggerData,
                                 String traceNamespace,
                                 String channel,
                                 CallSite callSite,
                                 String message) {
        String prefix = buildTraceMessagePrefix(loggerData, traceNamespace, channel, callSite);
        emitLine(loggerData, prefix, message, System.out);
    }

    public static void emitLog(LoggerData loggerData,
                               String traceNamespace,
                               LogSeverity severity,
                               CallSite callSite,
                               String message) {
        String prefix = buildLogMessagePrefix(loggerData, traceNamespace, severity, callSite);
        emitLine(loggerData, prefix, message, System.out);
    }

    public static String buildTraceMessagePrefix(LoggerData loggerData,
                                                 String traceNamespace,
                                                 String channel,
                                                 CallSite callSite) {
        StringBuilder out = new StringBuilder();
        appendNamespace(out, traceNamespace);
        appendTimestamp(loggerData, out);
        out.append('[').append(channel).append(']');
        appendLocation(loggerData, out, callSite);
        return out.toString();
    }

    public static String buildLogMessagePrefix(LoggerData loggerData,
                                               String traceNamespace,
                                               LogSeverity severity,
                                               CallSite callSite) {
        StringBuilder out = new StringBuilder();
        appendNamespace(out, traceNamespace);
        appendTimestamp(loggerData, out);
        out.append('[').append(severity.label()).append(']');
        appendLocation(loggerData, out, callSite);
        return out.toString();
    }

    public static String makeQualifiedChannelKey(String traceNamespace, String channel) {
        String namespaceName = trimWhitespace(traceNamespace);
        String channelName = trimWhitespace(channel);
        if (namespaceName.isEmpty() || channelName.isEmpty()) {
            return "";
        }
        return namespaceName + "." + channelName;
    }

    public static boolean isSelectorIdentifier(String token) {
        return !trimWhitespace(token).isEmpty() && IDENTIFIER.matcher(trimWhitespace(token)).matches();
    }

    public static boolean isValidChannelPath(String channel) {
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

    private static ChannelSpec findChannelSpec(TraceLoggerData data, String channelName) {
        for (ChannelSpec channel : data.channels()) {
            if (channel.name().equals(channelName)) {
                return channel;
            }
        }
        return null;
    }

    private static int mergeColor(int existingColor,
                                  int newColor,
                                  String traceNamespace,
                                  String channelName) {
        if (newColor == TraceColors.DEFAULT) {
            return existingColor;
        }
        if (newColor < 0 || newColor > 255) {
            throw new IllegalArgumentException("invalid trace color id '" + newColor + "'");
        }
        if (existingColor == TraceColors.DEFAULT) {
            return newColor;
        }
        if (existingColor != newColor) {
            throw new IllegalArgumentException(
                "conflicting trace color for '" + traceNamespace + "." + channelName + "'");
        }
        return existingColor;
    }

    private static List<Selector> parseSelectorList(String value,
                                                    String localNamespace,
                                                    List<String> invalidTokens) {
        List<Selector> selectors = new ArrayList<>();
        List<String> selectorTokens = new ArrayList<>();
        String splitError = splitByTopLevelCommas(value, selectorTokens);
        if (splitError != null) {
            invalidTokens.add(splitError);
            return selectors;
        }

        Set<String> invalidSeen = new LinkedHashSet<>();
        for (String token : selectorTokens) {
            String name = trimWhitespace(token);
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

    private static SelectorResolution resolveSelectorsToChannelKeys(LoggerData loggerData,
                                                                    List<Selector> selectors) {
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
                        String key = makeQualifiedChannelKey(traceNamespace, channel);
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

        return new SelectorResolution(channelKeys, unmatchedSelectors);
    }

    private static Selector parseSelector(String rawToken, String localNamespace) {
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
            traceNamespace = trimWhitespace(localNamespace);
            if (!isSelectorIdentifier(traceNamespace)) {
                throw new IllegalArgumentException("missing namespace");
            }
        } else if (isSelectorIdentifier(namespaceToken)) {
            traceNamespace = namespaceToken;
        } else {
            throw new IllegalArgumentException("invalid namespace '" + namespaceToken + "'");
        }

        List<String> tokens = splitChannelPath(channelPattern);
        if (tokens.isEmpty()) {
            throw new IllegalArgumentException("missing channel expression");
        }
        if (tokens.size() > 3) {
            throw new IllegalArgumentException("channel depth exceeds 3");
        }
        for (String token : tokens) {
            if (!token.equals("*") && !isSelectorIdentifier(token)) {
                throw new IllegalArgumentException("invalid channel token '" + token + "'");
            }
        }
        boolean includeTopLevel = tokens.size() == 2 && tokens.get(0).equals("*") && tokens.get(1).equals("*");
        return new Selector(anyNamespace, traceNamespace, List.copyOf(tokens), includeTopLevel);
    }

    private static boolean matchesSelector(Selector selector,
                                           String traceNamespace,
                                           String channel) {
        if (!selector.anyNamespace() && !traceNamespace.equals(selector.traceNamespace())) {
            return false;
        }
        List<String> channelParts = splitChannelPath(channel);
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

    private static List<String> splitChannelPath(String channel) {
        String token = trimWhitespace(channel);
        if (token.isEmpty()) {
            return List.of();
        }
        return Arrays.asList(token.split("\\."));
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
                parts.add(trimWhitespace(value.substring(start, index)));
                start = index + 1;
            }
        }
        if (braceDepth != 0) {
            return "unmatched '{'";
        }
        parts.add(trimWhitespace(value.substring(start)));
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

    private static String formatSelector(Selector selector) {
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

    private static void emitLine(LoggerData loggerData,
                                 String prefix,
                                 String message,
                                 PrintStream out) {
        synchronized (loggerData.outputLock) {
            out.print(prefix);
            out.print(' ');
            out.print(message);
            out.print('\n');
            out.flush();
        }
    }

    private static void appendNamespace(StringBuilder out, String traceNamespace) {
        if (traceNamespace == null || traceNamespace.isEmpty()) {
            return;
        }
        out.append('[').append(traceNamespace).append("] ");
    }

    private static void appendTimestamp(LoggerData loggerData, StringBuilder out) {
        if (!loggerData.timestampsEnabled) {
            return;
        }
        Instant now = Instant.now();
        out.append('[')
            .append(now.getEpochSecond())
            .append('.')
            .append(String.format(Locale.ROOT, "%06d", now.getNano() / 1000))
            .append("] ");
    }

    private static void appendLocation(LoggerData loggerData, StringBuilder out, CallSite callSite) {
        if (!loggerData.filenamesEnabled) {
            return;
        }
        out.append(" [").append(callSite.fileName());
        if (loggerData.lineNumbersEnabled && callSite.lineNumber() > 0) {
            out.append(':').append(callSite.lineNumber());
        }
        if (loggerData.functionNamesEnabled && callSite.methodName() != null && !callSite.methodName().isEmpty()) {
            out.append(':').append(callSite.methodName());
        }
        out.append(']');
    }

    private static String simplifyFileName(String fileName, String className) {
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
