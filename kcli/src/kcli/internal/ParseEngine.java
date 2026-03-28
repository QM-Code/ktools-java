package kcli.internal;

public final class ParseEngine {
    private ParseEngine() {
    }

    public static void parseOrExit(ParserData data, int argc, String[] argv) {
        try {
            parse(data, argc, argv);
        } catch (kcli.CliError ex) {
            Normalization.reportCliErrorAndExit(ex.getMessage());
        }
    }

    public static void parse(ParserData data, int argc, String[] argv) {
        new ParseSession(data, argc, argv).run();
    }
}
