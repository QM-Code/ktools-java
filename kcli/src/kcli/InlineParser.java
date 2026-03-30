package kcli;

import kcli.internal.InlineParserData;

public final class InlineParser {
    private final InlineParserData data;

    public InlineParser(String root) {
        this.data = new InlineParserData();
        data.setRoot(root);
    }

    public void setRoot(String root) {
        data.setRoot(root);
    }

    public void setRootValueHandler(ValueHandler handler) {
        data.setRootValueHandler(handler);
    }

    public void setRootValueHandler(ValueHandler handler,
                                    String valuePlaceholder,
                                    String description) {
        data.setRootValueHandler(handler, valuePlaceholder, description);
    }

    public void setHandler(String option,
                           FlagHandler handler,
                           String description) {
        data.setHandler(option, handler, description);
    }

    public void setHandler(String option,
                           ValueHandler handler,
                           String description) {
        data.setHandler(option, handler, description);
    }

    public void setOptionalValueHandler(String option,
                                        ValueHandler handler,
                                        String description) {
        data.setOptionalValueHandler(option, handler, description);
    }

    public InlineParser copy() {
        InlineParser copy = new InlineParser("--placeholder");
        copy.data.copyFrom(data.copy());
        return copy;
    }

    public InlineParserData snapshot() {
        return data.copy();
    }
}
