package tech.smartboot.feat.cloud.aot.orm.script;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public class TextSegment extends SqlSegment {

    /**
     * JDBC SQL
     */
    private final String sql;

    /**
     * 参数名称
     */
    private final List<String> parameters;

    public TextSegment(String sql, List<String> parameters) {
        this.sql = sql;
        this.parameters = new ArrayList<String>(parameters);
    }

    public String getSql() {
        return sql;
    }

    public List<String> getParameters() {
        return Collections.unmodifiableList(parameters);
    }

    @Override
    public String toString() {
        return sql;
    }
}