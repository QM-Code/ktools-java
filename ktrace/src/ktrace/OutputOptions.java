package ktrace;

public record OutputOptions(boolean filenames,
                            boolean lineNumbers,
                            boolean functionNames,
                            boolean timestamps) {
    public OutputOptions {
        if (!filenames) {
            lineNumbers = false;
            functionNames = false;
        }
    }
}
