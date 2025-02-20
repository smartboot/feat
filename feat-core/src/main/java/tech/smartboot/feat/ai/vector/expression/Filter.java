package tech.smartboot.feat.ai.vector.expression;

import java.util.function.Consumer;

public class Filter {
    public void build(Consumer<Filter> consumer) {
        consumer.accept(this);
    }
}
