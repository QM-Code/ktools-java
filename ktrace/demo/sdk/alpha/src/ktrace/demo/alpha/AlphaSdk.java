package ktrace.demo.alpha;

import ktrace.TraceColors;
import ktrace.TraceLogger;

public final class AlphaSdk {
    private AlphaSdk() {
    }

    public static TraceLogger getTraceLogger() {
        TraceLogger logger = Holder.TRACE_LOGGER;
        return logger;
    }

    public static void testTraceLoggingChannels() {
        TraceLogger trace = getTraceLogger();
        trace.trace("net", "testing...");
        trace.trace("net.alpha", "testing...");
        trace.trace("net.beta", "testing...");
        trace.trace("net.gamma", "testing...");
        trace.trace("net.gamma.deep", "testing...");
        trace.trace("cache", "testing...");
        trace.trace("cache.gamma", "testing...");
        trace.trace("cache.delta", "testing...");
        trace.trace("cache.special", "testing...");
    }

    public static void testStandardLoggingChannels() {
        TraceLogger trace = getTraceLogger();
        trace.info("testing...");
        trace.warn("testing...");
        trace.error("testing...");
    }

    private static final class Holder {
        private static final TraceLogger TRACE_LOGGER = buildTraceLogger();

        private Holder() {
        }

        private static TraceLogger buildTraceLogger() {
            TraceLogger logger = new TraceLogger("alpha");
            logger.addChannel("net", TraceColors.color("DeepSkyBlue1"));
            logger.addChannel("net.alpha");
            logger.addChannel("net.beta");
            logger.addChannel("net.gamma");
            logger.addChannel("net.gamma.deep");
            logger.addChannel("cache", TraceColors.color("Gold3"));
            logger.addChannel("cache.gamma", TraceColors.color("Gold3"));
            logger.addChannel("cache.delta");
            logger.addChannel("cache.special", TraceColors.color("Red"));
            return logger;
        }
    }
}
