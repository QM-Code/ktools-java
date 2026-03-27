package kcli.tests;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.PrintStream;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;

final class TestSupport {
    private TestSupport() {
    }

    static String captureStdout(Assertions.ThrowingRunnable runnable) throws Exception {
        PrintStream previous = System.out;
        ByteArrayOutputStream bytes = new ByteArrayOutputStream();
        try (PrintStream capture = new PrintStream(bytes, true, StandardCharsets.UTF_8)) {
            System.setOut(capture);
            runnable.run();
        } finally {
            System.setOut(previous);
        }
        return bytes.toString(StandardCharsets.UTF_8);
    }

    static ProcessResult runJava(String mainClass, String... args) throws IOException, InterruptedException {
        String javaBin = System.getProperty("java.home") + "/bin/java";
        String classpath = System.getProperty("java.class.path");
        List<String> command = new ArrayList<>();
        command.add(javaBin);
        command.add("-cp");
        command.add(classpath);
        command.add(mainClass);
        for (String arg : args) {
            command.add(arg);
        }

        Process process = new ProcessBuilder(command).start();
        byte[] stdout = process.getInputStream().readAllBytes();
        byte[] stderr = process.getErrorStream().readAllBytes();
        int exitCode = process.waitFor();
        return new ProcessResult(exitCode,
                                 new String(stdout, StandardCharsets.UTF_8),
                                 new String(stderr, StandardCharsets.UTF_8));
    }

    record ProcessResult(int exitCode, String stdout, String stderr) {
    }
}
