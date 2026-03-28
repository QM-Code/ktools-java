package ktrace.internal;

import java.lang.ref.WeakReference;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicBoolean;

import ktrace.OutputOptions;

public final class TraceInternals {
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
        final Object registryLock = new Object();
        final Object enabledLock = new Object();
        final Object outputLock = new Object();
        final AtomicBoolean hasEnabledChannels = new AtomicBoolean(false);
        volatile boolean filenamesEnabled;
        volatile boolean lineNumbersEnabled;
        volatile boolean functionNamesEnabled;
        volatile boolean timestampsEnabled;
        final Set<String> namespaces = new LinkedHashSet<>();
        final Map<String, List<String>> channelsByNamespace = new LinkedHashMap<>();
        final Map<String, Map<String, Integer>> colorsByNamespace = new LinkedHashMap<>();
        final Set<String> enabledChannelKeys = new LinkedHashSet<>();
        final List<TraceLoggerData> attachedTraceLoggers = new ArrayList<>();
    }

    public static int color(String colorName) {
        return TraceNaming.color(colorName);
    }

    public static List<String> colorNames() {
        return TraceNaming.colorNames();
    }

    public static String trimWhitespace(String value) {
        return TraceNaming.trimWhitespace(value);
    }

    public static String normalizeNamespace(String traceNamespace) {
        return TraceNaming.normalizeNamespace(traceNamespace);
    }

    public static String normalizeChannel(String channel) {
        return TraceNaming.normalizeChannel(channel);
    }

    public static void addChannel(TraceLoggerData data, String channel, int color) {
        TraceRegistry.addChannel(data, channel, color);
    }

    public static void ensureTraceLoggerCanAttach(TraceLoggerData traceLogger, LoggerData loggerData) {
        TraceRegistry.ensureTraceLoggerCanAttach(traceLogger, loggerData);
    }

    public static void attachTraceLogger(TraceLoggerData traceLogger, LoggerData loggerData) {
        TraceRegistry.attachTraceLogger(traceLogger, loggerData);
    }

    public static LoggerData attachedLogger(TraceLoggerData traceLogger) {
        return TraceRegistry.attachedLogger(traceLogger);
    }

    public static void mergeTraceLogger(LoggerData loggerData, TraceLoggerData traceLogger) {
        TraceRegistry.mergeTraceLogger(loggerData, traceLogger);
    }

    public static void retainTraceLogger(LoggerData loggerData, TraceLoggerData traceLogger) {
        TraceRegistry.retainTraceLogger(loggerData, traceLogger);
    }

    public static OutputOptions getOutputOptions(LoggerData loggerData) {
        return TraceRegistry.getOutputOptions(loggerData);
    }

    public static void setOutputOptions(LoggerData loggerData, OutputOptions options) {
        TraceRegistry.setOutputOptions(loggerData, options);
    }

    public static List<String> getNamespaces(LoggerData loggerData) {
        return TraceRegistry.getNamespaces(loggerData);
    }

    public static List<String> getChannels(LoggerData loggerData, String traceNamespace) {
        return TraceRegistry.getChannels(loggerData, traceNamespace);
    }

    public static boolean shouldTraceChannel(TraceLoggerData traceLogger, String channel) {
        return TraceRegistry.shouldTraceChannel(traceLogger, channel);
    }

    public static boolean shouldTraceQualifiedChannel(LoggerData loggerData,
                                                      String qualifiedChannel,
                                                      String localNamespace) {
        return TraceRegistry.shouldTraceQualifiedChannel(loggerData, qualifiedChannel, localNamespace);
    }

    public static boolean shouldTraceChannel(LoggerData loggerData,
                                             String traceNamespace,
                                             String channel) {
        return TraceRegistry.shouldTraceChannel(loggerData, traceNamespace, channel);
    }

    public static boolean isRegisteredTraceChannel(LoggerData loggerData,
                                                   String traceNamespace,
                                                   String channel) {
        return TraceRegistry.isRegisteredTraceChannel(loggerData, traceNamespace, channel);
    }

    public static void enableChannelKeys(LoggerData loggerData, List<String> channelKeys) {
        TraceRegistry.enableChannelKeys(loggerData, channelKeys);
    }

    public static void disableChannelKeys(LoggerData loggerData, List<String> channelKeys) {
        TraceRegistry.disableChannelKeys(loggerData, channelKeys);
    }

    public static ExactChannelResolution resolveExactChannel(LoggerData loggerData,
                                                             String qualifiedChannel,
                                                             String localNamespace) {
        return TraceSelectors.resolveExactChannel(loggerData, qualifiedChannel, localNamespace);
    }

    public static SelectorResolution resolveSelectorExpression(LoggerData loggerData,
                                                               String selectorsCsv,
                                                               String localNamespace) {
        return TraceSelectors.resolveSelectorExpression(loggerData, selectorsCsv, localNamespace);
    }

    public static CallSite captureCallSite() {
        return TraceFormatting.captureCallSite();
    }

    public static String makeTraceChangedSiteKey(String channel, CallSite callSite) {
        return TraceFormatting.makeTraceChangedSiteKey(channel, callSite);
    }

    public static String formatArgument(Object value) {
        return TraceFormatting.formatArgument(value);
    }

    public static String formatMessage(String formatText, Object... args) {
        return TraceFormatting.formatMessage(formatText, args);
    }

    public static void emitTrace(LoggerData loggerData,
                                 String traceNamespace,
                                 String channel,
                                 CallSite callSite,
                                 String message) {
        TraceFormatting.emitTrace(loggerData, traceNamespace, channel, callSite, message);
    }

    public static void emitLog(LoggerData loggerData,
                               String traceNamespace,
                               LogSeverity severity,
                               CallSite callSite,
                               String message) {
        TraceFormatting.emitLog(loggerData, traceNamespace, severity, callSite, message);
    }

    public static String buildTraceMessagePrefix(LoggerData loggerData,
                                                 String traceNamespace,
                                                 String channel,
                                                 CallSite callSite) {
        return TraceFormatting.buildTraceMessagePrefix(loggerData, traceNamespace, channel, callSite);
    }

    public static String buildLogMessagePrefix(LoggerData loggerData,
                                               String traceNamespace,
                                               LogSeverity severity,
                                               CallSite callSite) {
        return TraceFormatting.buildLogMessagePrefix(loggerData, traceNamespace, severity, callSite);
    }

    public static String makeQualifiedChannelKey(String traceNamespace, String channel) {
        return TraceNaming.makeQualifiedChannelKey(traceNamespace, channel);
    }

    public static boolean isSelectorIdentifier(String token) {
        return TraceNaming.isSelectorIdentifier(token);
    }

    public static boolean isValidChannelPath(String channel) {
        return TraceNaming.isValidChannelPath(channel);
    }
}
