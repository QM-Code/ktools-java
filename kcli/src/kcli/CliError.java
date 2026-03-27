package kcli;

public final class CliError extends RuntimeException {
    private final String option;

    public CliError(String option, String message) {
        super(message == null || message.isEmpty() ? "kcli parse failed" : message);
        this.option = option == null ? "" : option;
    }

    public String option() {
        return option;
    }
}
