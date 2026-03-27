package kcli;

import kcli.internal.InlineParserData;
import kcli.internal.Registration;

public final class InlineParser {
    private final InlineParserData data;

    public InlineParser(String root) {
        this.data = new InlineParserData();
        Registration.setInlineRoot(data, root);
    }

    public void setRoot(String root) {
        Registration.setInlineRoot(data, root);
    }

    public void setRootValueHandler(ValueHandler handler) {
        Registration.setRootValueHandler(data, handler);
    }

    public void setRootValueHandler(ValueHandler handler,
                                    String valuePlaceholder,
                                    String description) {
        Registration.setRootValueHandler(data, handler, valuePlaceholder, description);
    }

    public void setHandler(String option,
                           FlagHandler handler,
                           String description) {
        Registration.setInlineHandler(data, option, handler, description);
    }

    public void setHandler(String option,
                           ValueHandler handler,
                           String description) {
        Registration.setInlineHandler(data, option, handler, description);
    }

    public void setOptionalValueHandler(String option,
                                        ValueHandler handler,
                                        String description) {
        Registration.setInlineOptionalValueHandler(data, option, handler, description);
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
