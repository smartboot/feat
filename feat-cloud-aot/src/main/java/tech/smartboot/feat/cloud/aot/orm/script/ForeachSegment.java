package tech.smartboot.feat.cloud.aot.orm.script;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public class ForeachSegment extends ContainerSegment {

    private final String collection;

    private final String item;

    private final String open;

    private final String separator;

    private final String close;

    private final List<SqlSegment> segments =
            new ArrayList<SqlSegment>();

    public ForeachSegment(
            String collection,
            String item,
            String open,
            String separator,
            String close) {

        this.collection = collection;
        this.item = item;
        this.open = open;
        this.separator = separator;
        this.close = close;
    }

    public String getCollection() {
        return collection;
    }

    public String getItem() {
        return item;
    }

    public String getOpen() {
        return open;
    }

    public String getSeparator() {
        return separator;
    }

    public String getClose() {
        return close;
    }

    public void addSegment(SqlSegment segment) {
        segments.add(segment);
    }

    public List<SqlSegment> getSegments() {
        return Collections.unmodifiableList(segments);
    }
}