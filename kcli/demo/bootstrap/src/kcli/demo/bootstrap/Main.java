package kcli.demo.bootstrap;

import kcli.Parser;

public final class Main {
    private Main() {
    }

    public static void main(String[] args) {
        String[] argv = withProgram("kcli_demo_bootstrap", args);
        Parser parser = new Parser();
        parser.addAlias("-v", "--verbose");
        parser.setHandler("--verbose",
                          context -> System.out.println("Processing " + context.option()),
                          "Enable verbose demo logging.");
        parser.parseOrExit(argv.length, argv);
        System.out.println();
        System.out.println("KCLI java bootstrap import/parse check passed");
        System.out.println();
    }

    private static String[] withProgram(String programName, String[] args) {
        String[] argv = new String[(args == null ? 0 : args.length) + 1];
        argv[0] = programName;
        if (args != null && args.length > 0) {
            System.arraycopy(args, 0, argv, 1, args.length);
        }
        return argv;
    }
}
