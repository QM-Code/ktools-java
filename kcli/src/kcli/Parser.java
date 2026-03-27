package kcli;

import kcli.internal.ParseEngine;
import kcli.internal.ParserData;
import kcli.internal.Registration;

public final class Parser {
    private final ParserData data;

    public Parser() {
        this.data = new ParserData();
    }

    public void addAlias(String alias, String target) {
        Registration.setAlias(data, alias, target);
    }

    public void addAlias(String alias, String target, String... presetTokens) {
        Registration.setAlias(data, alias, target, presetTokens);
    }

    public void setHandler(String option,
                           FlagHandler handler,
                           String description) {
        Registration.setPrimaryHandler(data, option, handler, description);
    }

    public void setHandler(String option,
                           ValueHandler handler,
                           String description) {
        Registration.setPrimaryHandler(data, option, handler, description);
    }

    public void setOptionalValueHandler(String option,
                                        ValueHandler handler,
                                        String description) {
        Registration.setPrimaryOptionalValueHandler(data, option, handler, description);
    }

    public void setPositionalHandler(PositionalHandler handler) {
        Registration.setPositionalHandler(data, handler);
    }

    public void addInlineParser(InlineParser parser) {
        if (parser == null) {
            throw new IllegalArgumentException("kcli inline parser must not be empty");
        }
        Registration.addInlineParser(data, parser.snapshot());
    }

    public void parseOrExit(int argc, String[] argv) {
        ParseEngine.parseOrExit(data, argc, argv);
    }

    public void parseOrThrow(int argc, String[] argv) {
        ParseEngine.parse(data, argc, argv);
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
