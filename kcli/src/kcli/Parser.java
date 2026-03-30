package kcli;

import kcli.internal.ParserData;

public final class Parser {
    private final ParserData data;

    public Parser() {
        this.data = new ParserData();
    }

    public void addAlias(String alias, String target) {
        data.setAlias(alias, target);
    }

    public void addAlias(String alias, String target, String... presetTokens) {
        data.setAlias(alias, target, presetTokens);
    }

    public void setHandler(String option,
                           FlagHandler handler,
                           String description) {
        data.setPrimaryHandler(option, handler, description);
    }

    public void setHandler(String option,
                           ValueHandler handler,
                           String description) {
        data.setPrimaryHandler(option, handler, description);
    }

    public void setOptionalValueHandler(String option,
                                        ValueHandler handler,
                                        String description) {
        data.setPrimaryOptionalValueHandler(option, handler, description);
    }

    public void setPositionalHandler(PositionalHandler handler) {
        data.setPositionalHandler(handler);
    }

    public void addInlineParser(InlineParser parser) {
        if (parser == null) {
            throw new IllegalArgumentException("kcli inline parser must not be empty");
        }
        data.addInlineParser(parser.snapshot());
    }

    public void parseOrExit(int argc, String[] argv) {
        data.parseOrExit(argc, argv);
    }

    public void parseOrThrow(int argc, String[] argv) {
        data.parse(argc, argv);
    }

    public void parseOrExit(String[] args) {
        String[] argv = withProgramToken(args);
        parseOrExit(argv == null ? 0 : argv.length, argv);
    }

    public void parseOrThrow(String[] args) {
        String[] argv = withProgramToken(args);
        parseOrThrow(argv == null ? 0 : argv.length, argv);
    }

    private static String[] withProgramToken(String[] args) {
        if (args == null) {
            return null;
        }

        String[] argv = new String[args.length + 1];
        argv[0] = "";
        System.arraycopy(args, 0, argv, 1, args.length);
        return argv;
    }
}
