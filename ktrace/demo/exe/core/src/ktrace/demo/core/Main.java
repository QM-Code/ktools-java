package ktrace.demo.core;

import kcli.Parser;
import ktrace.Logger;
import ktrace.TraceColors;
import ktrace.TraceLogger;
import ktrace.demo.alpha.AlphaSdk;

public final class Main {
    private Main() {
    }

    public static void main(String[] args) {
        Logger logger = new Logger();

        TraceLogger trace = new TraceLogger("core");
        trace.addChannel("app", TraceColors.color("BrightCyan"));
        trace.addChannel("startup", TraceColors.color("BrightYellow"));

        logger.addTraceLogger(trace);
        logger.addTraceLogger(AlphaSdk.getTraceLogger());

        logger.enableChannel(trace, ".app");
        trace.trace("app", "core initialized local trace channels");

        Parser parser = new Parser();
        parser.addInlineParser(logger.makeInlineParser(trace));
        String[] argv = withProgram("ktrace_demo_core", args);
        parser.parseOrExit(argv.length, argv);

        trace.trace("app", "cli processing enabled, use --trace for options");
        trace.trace("startup", "testing imported tracing, use --trace '*.*' to view imported channels");
        AlphaSdk.testTraceLoggingChannels();
    }

    private static String[] withProgram(String programName, String[] args) {
        String[] argv = new String[(args == null ? 0 : args.length) + 1];
        argv[0] = programName;
        if (args != null && args.length > 0) {
            System.arraycopy(args, 0, argv, 1, args.length);
        }
        return argv;
    }
}
