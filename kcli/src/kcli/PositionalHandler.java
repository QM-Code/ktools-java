package kcli;

@FunctionalInterface
public interface PositionalHandler {
    void handle(HandlerContext context) throws Exception;
}
