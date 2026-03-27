package ktrace;

import java.util.List;

import ktrace.internal.TraceInternals;

public final class TraceColors {
    public static final int DEFAULT = 0xFFFF;

    private TraceColors() {
    }

    public static int color(String colorName) {
        return TraceInternals.color(colorName);
    }

    public static List<String> names() {
        return TraceInternals.colorNames();
    }
}
