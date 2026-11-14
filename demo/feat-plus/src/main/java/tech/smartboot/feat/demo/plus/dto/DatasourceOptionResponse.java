package tech.smartboot.feat.demo.plus.dto;

import java.util.ArrayList;
import java.util.List;

/**
 * {@code GET /api/pages/datasources} 的条目：配置管理页的数据源下拉选项，
 * 附带字段声明供编辑时提示「这个源有哪些字段可配」。
 */
public class DatasourceOptionResponse {
    private String key;
    private String label;
    private List<DwFieldMeta> fields = new ArrayList<>();

    public DatasourceOptionResponse() {
    }

    public DatasourceOptionResponse(String key, String label, List<DwFieldMeta> fields) {
        this.key = key;
        this.label = label;
        this.fields = fields == null ? new ArrayList<>() : fields;
    }

    public String getKey() {
        return key;
    }

    public void setKey(String key) {
        this.key = key;
    }

    public String getLabel() {
        return label;
    }

    public void setLabel(String label) {
        this.label = label;
    }

    public List<DwFieldMeta> getFields() {
        return fields;
    }

    public void setFields(List<DwFieldMeta> fields) {
        this.fields = fields == null ? new ArrayList<>() : fields;
    }
}
