package ktrace.internal;

import java.lang.ref.WeakReference;
import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import ktrace.OutputOptions;
import ktrace.TraceColors;

final class TraceRegistry {
    private TraceRegistry() {
    }

    static void addChannel(TraceInternals.TraceLoggerData data, String channel, int color) {
        String traceNamespace = TraceNaming.normalizeNamespace(data.traceNamespace());
        String channelName = TraceNaming.normalizeChannel(channel);
        int parentSeparator = channelName.lastIndexOf('.');
        if (parentSeparator >= 0) {
            String parentChannel = channelName.substring(0, parentSeparator);
            if (findChannelSpec(data, parentChannel) == null) {
                throw new IllegalArgumentException(
                    "cannot add unparented trace channel '" + channelName + "' (missing parent '" + parentChannel + "')");
            }
        }

        TraceInternals.ChannelSpec existing = findChannelSpec(data, channelName);
        if (existing != null) {
            int merged = mergeColor(existing.color(), color, traceNamespace, channelName);
            if (merged != existing.color()) {
                data.channels().set(data.channels().indexOf(existing), new TraceInternals.ChannelSpec(existing.name(), merged));
            }
            return;
        }

        if (color != TraceColors.DEFAULT && (color < 0 || color > 255)) {
            throw new IllegalArgumentException("invalid trace color id '" + color + "'");
        }
        data.channels().add(new TraceInternals.ChannelSpec(channelName, color));
    }

    static void ensureTraceLoggerCanAttach(TraceInternals.TraceLoggerData traceLogger,
                                           TraceInternals.LoggerData loggerData) {
        synchronized (traceLogger.attachedLoggerLock()) {
            TraceInternals.LoggerData attached = dereference(traceLogger.attachedLoggerRef()[0]);
            if (attached != null && attached != loggerData) {
                throw new IllegalArgumentException("trace logger is already attached to another logger");
            }
        }
    }

    static void attachTraceLogger(TraceInternals.TraceLoggerData traceLogger,
                                  TraceInternals.LoggerData loggerData) {
        synchronized (traceLogger.attachedLoggerLock()) {
            TraceInternals.LoggerData attached = dereference(traceLogger.attachedLoggerRef()[0]);
            if (attached != null && attached != loggerData) {
                throw new IllegalArgumentException("trace logger is already attached to another logger");
            }
            traceLogger.attachedLoggerRef()[0] = new WeakReference<>(loggerData);
        }
    }

    static TraceInternals.LoggerData attachedLogger(TraceInternals.TraceLoggerData traceLogger) {
        synchronized (traceLogger.attachedLoggerLock()) {
            return dereference(traceLogger.attachedLoggerRef()[0]);
        }
    }

    static void mergeTraceLogger(TraceInternals.LoggerData loggerData,
                                 TraceInternals.TraceLoggerData traceLogger) {
        String traceNamespace = TraceNaming.normalizeNamespace(traceLogger.traceNamespace());
        synchronized (loggerData.registryLock) {
            loggerData.namespaces.add(traceNamespace);
            List<String> registeredChannels =
                loggerData.channelsByNamespace.computeIfAbsent(traceNamespace, ignored -> new ArrayList<>());
            Map<String, Integer> registeredColors =
                loggerData.colorsByNamespace.computeIfAbsent(traceNamespace, ignored -> new LinkedHashMap<>());

            for (TraceInternals.ChannelSpec channel : traceLogger.channels()) {
                String channelName = TraceNaming.normalizeChannel(channel.name());
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

    static void retainTraceLogger(TraceInternals.LoggerData loggerData,
                                  TraceInternals.TraceLoggerData traceLogger) {
        synchronized (loggerData.registryLock) {
            if (!loggerData.attachedTraceLoggers.contains(traceLogger)) {
                loggerData.attachedTraceLoggers.add(traceLogger);
            }
        }
    }

    static OutputOptions getOutputOptions(TraceInternals.LoggerData loggerData) {
        return new OutputOptions(
            loggerData.filenamesEnabled,
            loggerData.lineNumbersEnabled,
            loggerData.functionNamesEnabled,
            loggerData.timestampsEnabled);
    }

    static void setOutputOptions(TraceInternals.LoggerData loggerData, OutputOptions options) {
        loggerData.filenamesEnabled = options.filenames();
        loggerData.lineNumbersEnabled = options.filenames() && options.lineNumbers();
        loggerData.functionNamesEnabled = options.filenames() && options.functionNames();
        loggerData.timestampsEnabled = options.timestamps();
    }

    static List<String> getNamespaces(TraceInternals.LoggerData loggerData) {
        synchronized (loggerData.registryLock) {
            List<String> namespaces = new ArrayList<>(loggerData.namespaces);
            Collections.sort(namespaces);
            return namespaces;
        }
    }

    static List<String> getChannels(TraceInternals.LoggerData loggerData, String traceNamespace) {
        String namespaceName = TraceNaming.normalizeNamespace(traceNamespace);
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

    static boolean shouldTraceChannel(TraceInternals.TraceLoggerData traceLogger, String channel) {
        try {
            TraceInternals.LoggerData logger = attachedLogger(traceLogger);
            if (logger == null) {
                return false;
            }
            return shouldTraceChannel(logger, traceLogger.traceNamespace(), TraceNaming.normalizeChannel(channel));
        } catch (RuntimeException ex) {
            return false;
        }
    }

    static boolean shouldTraceQualifiedChannel(TraceInternals.LoggerData loggerData,
                                               String qualifiedChannel,
                                               String localNamespace) {
        try {
            TraceInternals.ExactChannelResolution resolution =
                TraceSelectors.resolveExactChannel(loggerData, qualifiedChannel, localNamespace);
            return shouldTraceChannel(loggerData, resolution.traceNamespace(), resolution.channel());
        } catch (RuntimeException ex) {
            return false;
        }
    }

    static boolean shouldTraceChannel(TraceInternals.LoggerData loggerData,
                                      String traceNamespace,
                                      String channel) {
        if (!TraceNaming.isValidChannelPath(channel) || !loggerData.hasEnabledChannels.get()) {
            return false;
        }
        if (!isRegisteredTraceChannel(loggerData, traceNamespace, channel)) {
            return false;
        }
        String key = TraceNaming.makeQualifiedChannelKey(traceNamespace, channel);
        synchronized (loggerData.enabledLock) {
            return loggerData.enabledChannelKeys.contains(key);
        }
    }

    static boolean isRegisteredTraceChannel(TraceInternals.LoggerData loggerData,
                                            String traceNamespace,
                                            String channel) {
        String namespaceName = TraceNaming.trimWhitespace(traceNamespace);
        String channelName = TraceNaming.trimWhitespace(channel);
        if (!TraceNaming.isSelectorIdentifier(namespaceName) || !TraceNaming.isValidChannelPath(channelName)) {
            return false;
        }

        synchronized (loggerData.registryLock) {
            List<String> channels = loggerData.channelsByNamespace.get(namespaceName);
            return channels != null && channels.contains(channelName);
        }
    }

    static void enableChannelKeys(TraceInternals.LoggerData loggerData, List<String> channelKeys) {
        synchronized (loggerData.enabledLock) {
            for (String key : channelKeys) {
                if (key != null && !key.isEmpty()) {
                    loggerData.enabledChannelKeys.add(key);
                }
            }
            loggerData.hasEnabledChannels.set(!loggerData.enabledChannelKeys.isEmpty());
        }
    }

    static void disableChannelKeys(TraceInternals.LoggerData loggerData, List<String> channelKeys) {
        synchronized (loggerData.enabledLock) {
            loggerData.enabledChannelKeys.removeAll(channelKeys);
            loggerData.hasEnabledChannels.set(!loggerData.enabledChannelKeys.isEmpty());
        }
    }

    private static TraceInternals.ChannelSpec findChannelSpec(TraceInternals.TraceLoggerData data, String channelName) {
        for (TraceInternals.ChannelSpec channel : data.channels()) {
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

    private static TraceInternals.LoggerData dereference(WeakReference<TraceInternals.LoggerData> reference) {
        return reference == null ? null : reference.get();
    }
}
