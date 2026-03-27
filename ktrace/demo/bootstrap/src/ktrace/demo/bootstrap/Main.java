package ktrace.demo.bootstrap;

import ktrace.Logger;
import ktrace.TraceLogger;

public final class Main {
    private Main() {
    }

    public static void main(String[] args) {
        Logger logger = new Logger();
        TraceLogger trace = new TraceLogger("bootstrap");
        trace.addChannel("app");
        logger.addTraceLogger(trace);
        logger.enableChannel(trace, ".app");
        trace.trace("app", "KTRACE java bootstrap import/parse check passed");
    }
}
