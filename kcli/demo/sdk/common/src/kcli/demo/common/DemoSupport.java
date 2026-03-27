package kcli.demo.common;

import java.util.stream.Collectors;

import kcli.HandlerContext;

public final class DemoSupport {
    private DemoSupport() {
    }

    public static void printProcessingLine(HandlerContext context, String value) {
        if (context.valueTokens().isEmpty()) {
            System.out.println("Processing " + context.option());
            return;
        }

        if (context.valueTokens().size() == 1) {
            System.out.println("Processing " + context.option() + " with value \"" + value + "\"");
            return;
        }

        String joined = context.valueTokens()
            .stream()
            .map(token -> "\"" + token + "\"")
            .collect(Collectors.joining(","));
        System.out.println("Processing " + context.option() + " with values [" + joined + "]");
    }

    public static String[] withProgram(String programName, String[] args) {
        String[] argv = new String[(args == null ? 0 : args.length) + 1];
        argv[0] = programName;
        if (args != null && args.length > 0) {
            System.arraycopy(args, 0, argv, 1, args.length);
        }
        return argv;
    }
}
