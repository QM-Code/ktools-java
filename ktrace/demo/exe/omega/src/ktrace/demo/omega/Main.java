package ktrace.demo.omega;

import kcli.Parser;
import ktrace.Logger;
import ktrace.TraceColors;
import ktrace.TraceLogger;
import ktrace.demo.alpha.AlphaSdk;
import ktrace.demo.beta.BetaSdk;
import ktrace.demo.gamma.GammaSdk;

import static ktrace.demo.common.DemoSupport.withProgram;

public final class Main {
    private Main() {
    }

    public static void main(String[] args) {
        Logger logger = new Logger();

        TraceLogger trace = new TraceLogger("omega");
        trace.addChannel("app", TraceColors.color("BrightCyan"));
        trace.addChannel("orchestrator", TraceColors.color("BrightYellow"));
        trace.addChannel("deep");
        trace.addChannel("deep.branch");
        trace.addChannel("deep.branch.leaf", TraceColors.color("LightSalmon1"));

        logger.addTraceLogger(trace);
        logger.addTraceLogger(AlphaSdk.getTraceLogger());
        logger.addTraceLogger(BetaSdk.getTraceLogger());
        logger.addTraceLogger(GammaSdk.getTraceLogger());

        logger.enableChannel(trace, ".app");
        trace.trace("app", "omega initialized local trace channels");
        logger.disableChannel(trace, ".app");

        Parser parser = new Parser();
        parser.addInlineParser(logger.makeInlineParser(trace));
        String[] argv = withProgram("ktrace_demo_omega", args);
        parser.parseOrExit(argv.length, argv);

        trace.trace("app", "cli processing enabled, use --trace for options");
        trace.trace("app", "testing external tracing, use --trace '*.*' to view top-level channels");
        trace.trace("deep.branch.leaf", "omega trace test on channel 'deep.branch.leaf'");
        AlphaSdk.testTraceLoggingChannels();
        BetaSdk.testTraceLoggingChannels();
        GammaSdk.testTraceLoggingChannels();
        AlphaSdk.testStandardLoggingChannels();
        trace.trace("orchestrator", "omega completed imported SDK trace checks");
        trace.info("testing...");
        trace.warn("testing...");
        trace.error("testing...");
    }
}
