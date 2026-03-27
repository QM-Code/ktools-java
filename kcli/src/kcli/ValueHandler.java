package kcli;

@FunctionalInterface
public interface ValueHandler {
    void handle(HandlerContext context, String value) throws Exception;
}
