package ktrace;

import java.util.List;

import kcli.HandlerContext;
import kcli.InlineParser;
import ktrace.internal.TraceInternals;
import ktrace.internal.TraceInternals.CallSite;
import ktrace.internal.TraceInternals.LogSeverity;
import ktrace.internal.TraceInternals.LoggerData;
import ktrace.internal.TraceInternals.SelectorResolution;
import ktrace.internal.TraceInternals.TraceLoggerData;

public final class Logger {
    private final LoggerData data;
    private final TraceLogger internalTrace;

    public Logger() {
        this.data = new LoggerData();
        this.internalTrace = new TraceLogger("ktrace");
        configureInternalTrace(internalTrace);
        addTraceLogger(internalTrace);
    }

    public void addTraceLogger(TraceLogger logger) {
        if (logger == null) {
            throw new IllegalArgumentException("ktrace trace logger must not be empty");
        }

        TraceLoggerData traceData = logger.data;
        TraceInternals.ensureTraceLoggerCanAttach(traceData, data);
        TraceInternals.mergeTraceLogger(data, traceData);
        TraceInternals.retainTraceLogger(data, traceData);
        TraceInternals.attachTraceLogger(traceData, data);
    }

    public void enableChannel(String qualifiedChannel) {
        enableChannel(qualifiedChannel, "");
    }

    public void enableChannel(TraceLogger localTraceLogger, String qualifiedChannel) {
        enableChannel(qualifiedChannel, localTraceLogger == null ? "" : localTraceLogger.getNamespace());
    }

    public void enableChannels(String selectorsCsv) {
        enableChannels(selectorsCsv, "");
    }

    public void enableChannels(TraceLogger localTraceLogger, String selectorsCsv) {
        enableChannels(selectorsCsv, localTraceLogger == null ? "" : localTraceLogger.getNamespace());
    }

    public boolean shouldTraceChannel(String qualifiedChannel) {
        return shouldTraceChannel(qualifiedChannel, "");
    }

    public boolean shouldTraceChannel(TraceLogger localTraceLogger, String qualifiedChannel) {
        return shouldTraceChannel(qualifiedChannel, localTraceLogger == null ? "" : localTraceLogger.getNamespace());
    }

    public void disableChannel(String qualifiedChannel) {
        disableChannel(qualifiedChannel, "");
    }

    public void disableChannel(TraceLogger localTraceLogger, String qualifiedChannel) {
        disableChannel(qualifiedChannel, localTraceLogger == null ? "" : localTraceLogger.getNamespace());
    }

    public void disableChannels(String selectorsCsv) {
        disableChannels(selectorsCsv, "");
    }

    public void disableChannels(TraceLogger localTraceLogger, String selectorsCsv) {
        disableChannels(selectorsCsv, localTraceLogger == null ? "" : localTraceLogger.getNamespace());
    }

    public void setOutputOptions(OutputOptions options) {
        TraceInternals.setOutputOptions(data, options);
        internalTrace.trace("api", "updating output options (enable api.output for details)");
        OutputOptions next = getOutputOptions();
        internalTrace.trace("api.output",
            "set output options: filenames={} line_numbers={} function_names={} timestamps={}",
            next.filenames(),
            next.lineNumbers(),
            next.functionNames(),
            next.timestamps());
    }

    public OutputOptions getOutputOptions() {
        return TraceInternals.getOutputOptions(data);
    }

    public List<String> getNamespaces() {
        return TraceInternals.getNamespaces(data);
    }

    public List<String> getChannels(String traceNamespace) {
        return TraceInternals.getChannels(data, traceNamespace);
    }

    public InlineParser makeInlineParser(TraceLogger localTraceLogger) {
        return makeInlineParser(localTraceLogger, "trace");
    }

    public InlineParser makeInlineParser(TraceLogger localTraceLogger, String traceRoot) {
        String localNamespace = localTraceLogger == null ? "" : localTraceLogger.getNamespace();

        InlineParser parser = new InlineParser(traceRoot == null || traceRoot.isBlank() ? "trace" : traceRoot);
        parser.setRootValueHandler((context, value) -> enableChannels(value, localNamespace),
            "<channels>",
            "Trace selected channels.");
        parser.setHandler("-examples", this::handleExamples, "Show selector examples.");
        parser.setHandler("-namespaces", context -> handleNamespaces(), "Show initialized trace namespaces.");
        parser.setHandler("-channels", context -> handleChannels(), "Show initialized trace channels.");
        parser.setHandler("-colors", context -> handleColors(), "Show available trace colors.");
        parser.setHandler("-files", context -> setOutputOptions(new OutputOptions(true, true, false, getOutputOptions().timestamps())),
            "Include source file and line in trace output.");
        parser.setHandler("-functions", context -> setOutputOptions(new OutputOptions(true, true, true, getOutputOptions().timestamps())),
            "Include function names in trace output.");
        parser.setHandler("-timestamps", context -> {
            OutputOptions options = getOutputOptions();
            setOutputOptions(new OutputOptions(options.filenames(), options.lineNumbers(), options.functionNames(), true));
        }, "Include timestamps in trace output.");
        return parser;
    }

    private void enableChannel(String qualifiedChannel, String localNamespace) {
        TraceInternals.ExactChannelResolution resolution =
            TraceInternals.resolveExactChannel(data, qualifiedChannel, localNamespace);
        if (!resolution.registered()) {
            log(LogSeverity.WARNING,
                localNamespace,
                TraceInternals.captureCallSite(),
                TraceInternals.formatMessage("enable ignored channel '{}' because it is not registered",
                    resolution.key()));
            return;
        }

        TraceInternals.enableChannelKeys(data, List.of(resolution.key()));
        internalTrace.trace("api.channels", "enabled channel '{}'", TraceInternals.trimWhitespace(qualifiedChannel));
    }

    private void enableChannels(String selectorsCsv, String localNamespace) {
        String selectorText = TraceInternals.trimWhitespace(selectorsCsv);
        SelectorResolution resolution = TraceInternals.resolveSelectorExpression(data, selectorText, localNamespace);
        TraceInternals.enableChannelKeys(data, resolution.channelKeys());
        CallSite callSite = TraceInternals.captureCallSite();
        for (String unmatched : resolution.unmatchedSelectors()) {
            log(LogSeverity.WARNING,
                localNamespace,
                callSite,
                TraceInternals.formatMessage(
                    "enable ignored channel selector '{}' because it matched no registered channels",
                    unmatched));
        }

        internalTrace.trace("api",
            "processing channels (enable api.channels for details): enabled {} channel(s), {} unmatched selector(s)",
            resolution.channelKeys().size(),
            resolution.unmatchedSelectors().size());
        internalTrace.trace("api.channels",
            "enabled {} channel(s) from '{}' ({} unmatched selector(s))",
            resolution.channelKeys().size(),
            selectorText,
            resolution.unmatchedSelectors().size());
    }

    private boolean shouldTraceChannel(String qualifiedChannel, String localNamespace) {
        return TraceInternals.shouldTraceQualifiedChannel(data, qualifiedChannel, localNamespace);
    }

    private void disableChannel(String qualifiedChannel, String localNamespace) {
        TraceInternals.ExactChannelResolution resolution =
            TraceInternals.resolveExactChannel(data, qualifiedChannel, localNamespace);
        if (!resolution.registered()) {
            log(LogSeverity.WARNING,
                localNamespace,
                TraceInternals.captureCallSite(),
                TraceInternals.formatMessage("disable ignored channel '{}' because it is not registered",
                    resolution.key()));
            return;
        }

        TraceInternals.disableChannelKeys(data, List.of(resolution.key()));
        internalTrace.trace("api.channels", "disabled channel '{}'", TraceInternals.trimWhitespace(qualifiedChannel));
    }

    private void disableChannels(String selectorsCsv, String localNamespace) {
        String selectorText = TraceInternals.trimWhitespace(selectorsCsv);
        SelectorResolution resolution = TraceInternals.resolveSelectorExpression(data, selectorText, localNamespace);
        TraceInternals.disableChannelKeys(data, resolution.channelKeys());
        CallSite callSite = TraceInternals.captureCallSite();
        for (String unmatched : resolution.unmatchedSelectors()) {
            log(LogSeverity.WARNING,
                localNamespace,
                callSite,
                TraceInternals.formatMessage(
                    "disable ignored channel selector '{}' because it matched no registered channels",
                    unmatched));
        }

        internalTrace.trace("api",
            "processing channels (enable api.channels for details): disabled {} channel(s), {} unmatched selector(s)",
            resolution.channelKeys().size(),
            resolution.unmatchedSelectors().size());
        internalTrace.trace("api.channels",
            "disabled {} channel(s) from '{}' ({} unmatched selector(s))",
            resolution.channelKeys().size(),
            selectorText,
            resolution.unmatchedSelectors().size());
    }

    private void log(LogSeverity severity, String traceNamespace, CallSite callSite, String message) {
        TraceInternals.emitLog(data, traceNamespace, severity, callSite, message);
    }

    private void handleExamples(HandlerContext context) {
        String optionRoot = "--" + context.root();
        System.out.println();
        System.out.println("General trace selector pattern:");
        System.out.println("  " + optionRoot + " <namespace>.<channel>[.<subchannel>[.<subchannel>]]");
        System.out.println();
        System.out.println("Trace selector examples:");
        System.out.println("  " + optionRoot + " '.abc'           Select local 'abc' in current namespace");
        System.out.println("  " + optionRoot + " '.abc.xyz'       Select local nested channel in current namespace");
        System.out.println("  " + optionRoot + " 'otherapp.channel' Select explicit namespace channel");
        System.out.println("  " + optionRoot + " '*.*'            Select all <namespace>.<channel> channels");
        System.out.println("  " + optionRoot + " '*.*.*'          Select all channels up to 2 levels");
        System.out.println("  " + optionRoot + " '*.*.*.*'        Select all channels up to 3 levels");
        System.out.println("  " + optionRoot + " 'alpha.*'        Select all top-level channels in alpha");
        System.out.println("  " + optionRoot + " 'alpha.*.*'      Select all channels in alpha (up to 2 levels)");
        System.out.println("  " + optionRoot + " 'alpha.*.*.*'    Select all channels in alpha (up to 3 levels)");
        System.out.println("  " + optionRoot + " '*.net'          Select 'net' across all namespaces");
        System.out.println("  " + optionRoot + " '*.scheduler.tick' Select 'scheduler.tick' across namespaces");
        System.out.println("  " + optionRoot + " '*.net.*'        Select subchannels under 'net' across namespaces");
        System.out.println("  " + optionRoot + " '*.{net,io}'     Select 'net' and 'io' across all namespaces");
        System.out.println("  " + optionRoot + " '{alpha,beta}.*' Select all top-level channels in alpha and beta");
        System.out.println();
    }

    private void handleNamespaces() {
        List<String> namespaces = getNamespaces();
        if (namespaces.isEmpty()) {
            System.out.println("No trace namespaces defined.");
            System.out.println();
            return;
        }

        System.out.println();
        System.out.println("Available trace namespaces:");
        for (String traceNamespace : namespaces) {
            System.out.println("  " + traceNamespace);
        }
        System.out.println();
    }

    private void handleChannels() {
        boolean printedAny = false;
        for (String traceNamespace : getNamespaces()) {
            for (String channel : getChannels(traceNamespace)) {
                if (!printedAny) {
                    System.out.println();
                    System.out.println("Available trace channels:");
                    printedAny = true;
                }
                System.out.println("  " + traceNamespace + "." + channel);
            }
        }
        if (!printedAny) {
            System.out.println("No trace channels defined.");
            System.out.println();
            return;
        }
        System.out.println();
    }

    private void handleColors() {
        System.out.println();
        System.out.println("Available trace colors:");
        for (String name : TraceColors.names()) {
            System.out.println("  " + name);
        }
        System.out.println();
    }

    private static void configureInternalTrace(TraceLogger logger) {
        logger.addChannel("api", 6);
        logger.addChannel("api.channels");
        logger.addChannel("api.cli");
        logger.addChannel("api.output");
        logger.addChannel("selector", 3);
        logger.addChannel("selector.parse");
        logger.addChannel("registry", 5);
        logger.addChannel("registry.query");
    }
}
