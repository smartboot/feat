package tech.smartboot.feat.cloud.aot.orm.script;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public class IfSegment extends ContainerSegment {

    /**
     * id != null
     */
    private final String test;

    private final List<SqlSegment> segments =
            new ArrayList<SqlSegment>();

    public IfSegment(String test) {
        this.test = test;
    }

    public String getTest() {
        return test;
    }

    public void addSegment(SqlSegment segment) {
        segments.add(segment);
    }

    public List<SqlSegment> getSegments() {
        return Collections.unmodifiableList(segments);
    }
}