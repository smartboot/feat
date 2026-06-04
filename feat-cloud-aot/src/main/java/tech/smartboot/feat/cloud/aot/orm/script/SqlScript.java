package tech.smartboot.feat.cloud.aot.orm.script;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public class SqlScript {

    private final List<SqlSegment> segments = new ArrayList<SqlSegment>();

    public void addSegment(SqlSegment segment) {
        segments.add(segment);
    }

    public List<SqlSegment> getSegments() {
        return Collections.unmodifiableList(segments);
    }
}