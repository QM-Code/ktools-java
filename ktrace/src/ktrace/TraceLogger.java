package ktrace;

import java.util.Objects;

import ktrace.internal.TraceInternals;
import ktrace.internal.TraceInternals.CallSite;
import ktrace.internal.TraceInternals.LogSeverity;
import ktrace.internal.TraceInternals.TraceLoggerData;

public final class TraceLogger {
    final TraceLoggerData data;

    public TraceLogger(String traceNamespace) {
        this.data = new TraceLoggerData(TraceInternals.normalizeNamespace(traceNamespace));
    }

    public void addChannel(String channel) {
        addChannel(channel, TraceColors.DEFAULT);
    }

    public void addChannel(String channel, int color) {
        TraceInternals.addChannel(data, channel, color);
    }

    public String getNamespace() {
        return data.traceNamespace();
    }

    public boolean shouldTraceChannel(String channel) {
        return TraceInternals.shouldTraceChannel(data, channel);
    }

    public void trace(String channel, String formatText, Object... args) {
        String normalizedChannel = TraceInternals.normalizeChannel(channel);
        TraceInternals.LoggerData logger = TraceInternals.attachedLogger(data);
        if (logger == null || !TraceInternals.shouldTraceChannel(logger, data.traceNamespace(), normalizedChannel)) {
            return;
        }

        CallSite callSite = TraceInternals.captureCallSite();
        String message = TraceInternals.formatMessage(formatText, args);
        TraceInternals.emitTrace(logger, data.traceNamespace(), normalizedChannel, callSite, message);
    }

    public void traceChanged(String channel, Object keyExpr, String formatText, Object... args) {
        String normalizedChannel = TraceInternals.normalizeChannel(channel);
        CallSite callSite = TraceInternals.captureCallSite();
        String siteKey = TraceInternals.makeTraceChangedSiteKey(normalizedChannel, callSite);
        String nextKey = TraceInternals.formatArgument(keyExpr);
        String previous = data.changedKeys().put(siteKey, nextKey);
        if (Objects.equals(previous, nextKey)) {
            return;
        }
        trace(channel, formatText, args);
    }

    public void info(String formatText, Object... args) {
        log(LogSeverity.INFO, formatText, args);
    }

    public void warn(String formatText, Object... args) {
        log(LogSeverity.WARNING, formatText, args);
    }

    public void error(String formatText, Object... args) {
        log(LogSeverity.ERROR, formatText, args);
    }

    private void log(LogSeverity severity, String formatText, Object... args) {
        TraceInternals.LoggerData logger = TraceInternals.attachedLogger(data);
        if (logger == null) {
            return;
        }

        CallSite callSite = TraceInternals.captureCallSite();
        String message = TraceInternals.formatMessage(formatText, args);
        TraceInternals.emitLog(logger, data.traceNamespace(), severity, callSite, message);
    }
}
