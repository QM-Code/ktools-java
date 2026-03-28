package ktrace.internal;

import java.io.PrintStream;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Objects;

final class TraceFormatting {
    private TraceFormatting() {
    }

    static TraceInternals.CallSite captureCallSite() {
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
            return new TraceInternals.CallSite(
                TraceNaming.simplifyFileName(frame.getFileName(), className),
                frame.getLineNumber(),
                frame.getMethodName());
        }
        return new TraceInternals.CallSite("unknown", -1, "");
    }

    static String makeTraceChangedSiteKey(String channel, TraceInternals.CallSite callSite) {
        return callSite.fileName() + ":" + callSite.lineNumber() + ":" + callSite.methodName() + ":" + channel;
    }

    static String formatArgument(Object value) {
        return String.valueOf(value);
    }

    static String formatMessage(String formatText, Object... args) {
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

    static void emitTrace(TraceInternals.LoggerData loggerData,
                          String traceNamespace,
                          String channel,
                          TraceInternals.CallSite callSite,
                          String message) {
        String prefix = buildTraceMessagePrefix(loggerData, traceNamespace, channel, callSite);
        emitLine(loggerData, prefix, message, System.out);
    }

    static void emitLog(TraceInternals.LoggerData loggerData,
                        String traceNamespace,
                        TraceInternals.LogSeverity severity,
                        TraceInternals.CallSite callSite,
                        String message) {
        String prefix = buildLogMessagePrefix(loggerData, traceNamespace, severity, callSite);
        emitLine(loggerData, prefix, message, System.out);
    }

    static String buildTraceMessagePrefix(TraceInternals.LoggerData loggerData,
                                          String traceNamespace,
                                          String channel,
                                          TraceInternals.CallSite callSite) {
        StringBuilder out = new StringBuilder();
        appendNamespace(out, traceNamespace);
        appendTimestamp(loggerData, out);
        out.append('[').append(channel).append(']');
        appendLocation(loggerData, out, callSite);
        return out.toString();
    }

    static String buildLogMessagePrefix(TraceInternals.LoggerData loggerData,
                                        String traceNamespace,
                                        TraceInternals.LogSeverity severity,
                                        TraceInternals.CallSite callSite) {
        StringBuilder out = new StringBuilder();
        appendNamespace(out, traceNamespace);
        appendTimestamp(loggerData, out);
        out.append('[').append(severity.label()).append(']');
        appendLocation(loggerData, out, callSite);
        return out.toString();
    }

    private static void emitLine(TraceInternals.LoggerData loggerData,
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

    private static void appendTimestamp(TraceInternals.LoggerData loggerData, StringBuilder out) {
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

    private static void appendLocation(TraceInternals.LoggerData loggerData,
                                       StringBuilder out,
                                       TraceInternals.CallSite callSite) {
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
}
