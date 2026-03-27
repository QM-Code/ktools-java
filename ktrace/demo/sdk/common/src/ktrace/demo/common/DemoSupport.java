package ktrace.demo.common;

public final class DemoSupport {
    private DemoSupport() {
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
