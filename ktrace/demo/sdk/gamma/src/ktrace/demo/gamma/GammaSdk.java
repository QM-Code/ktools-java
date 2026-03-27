package ktrace.demo.gamma;

import ktrace.TraceColors;
import ktrace.TraceLogger;

public final class GammaSdk {
    private GammaSdk() {
    }

    public static TraceLogger getTraceLogger() {
        return Holder.TRACE_LOGGER;
    }

    public static void testTraceLoggingChannels() {
        TraceLogger trace = getTraceLogger();
        trace.trace("physics", "gamma trace test on channel 'physics'");
        trace.trace("metrics", "gamma trace test on channel 'metrics'");
    }

    private static final class Holder {
        private static final TraceLogger TRACE_LOGGER = buildTraceLogger();

        private Holder() {
        }

        private static TraceLogger buildTraceLogger() {
            TraceLogger logger = new TraceLogger("gamma");
            logger.addChannel("physics", TraceColors.color("MediumOrchid1"));
            logger.addChannel("metrics", TraceColors.color("LightSkyBlue1"));
            return logger;
        }
    }
}
