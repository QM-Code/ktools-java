package kcli;

import java.util.List;

public record HandlerContext(String root,
                             String option,
                             String command,
                             List<String> valueTokens) {
    public HandlerContext {
        root = root == null ? "" : root;
        option = option == null ? "" : option;
        command = command == null ? "" : command;
        valueTokens = valueTokens == null ? List.of() : List.copyOf(valueTokens);
    }
}
