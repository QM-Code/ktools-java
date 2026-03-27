package ktrace.demo.beta;

import ktrace.TraceColors;
import ktrace.TraceLogger;

public final class BetaSdk {
    private BetaSdk() {
    }

    public static TraceLogger getTraceLogger() {
        return Holder.TRACE_LOGGER;
    }

    public static void testTraceLoggingChannels() {
        TraceLogger trace = getTraceLogger();
        trace.trace("io", "beta trace test on channel 'io'");
        trace.trace("scheduler", "beta trace test on channel 'scheduler'");
    }

    private static final class Holder {
        private static final TraceLogger TRACE_LOGGER = buildTraceLogger();

        private Holder() {
        }

        private static TraceLogger buildTraceLogger() {
            TraceLogger logger = new TraceLogger("beta");
            logger.addChannel("io", TraceColors.color("MediumSpringGreen"));
            logger.addChannel("scheduler", TraceColors.color("Orange3"));
            return logger;
        }
    }
}
