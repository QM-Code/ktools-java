package kcli;

@FunctionalInterface
public interface FlagHandler {
    void handle(HandlerContext context) throws Exception;
}
