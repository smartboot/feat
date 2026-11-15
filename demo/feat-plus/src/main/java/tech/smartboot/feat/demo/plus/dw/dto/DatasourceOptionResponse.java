package tech.smartboot.feat.demo.plus.dw.dto;

import java.util.ArrayList;
import java.util.List;

/**
 * {@code GET /api/pages/datasources} 的条目：配置管理页的数据源下拉选项，
 * 附带字段声明供编辑时提示「这个源有哪些字段可配」，
 * writable 供编辑器禁用只读数据源的 create / edit / delete 动作勾选。
 */
public class DatasourceOptionResponse {
    private String key;
    private String label;
    private boolean writable;
    private List<DwFieldMeta> fields = new ArrayList<>();

    public DatasourceOptionResponse() {
    }

    public DatasourceOptionResponse(String key, String label, boolean writable, List<DwFieldMeta> fields) {
        this.key = key;
        this.label = label;
        this.writable = writable;
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

    public boolean isWritable() {
        return writable;
    }

    public void setWritable(boolean writable) {
        this.writable = writable;
    }

    public List<DwFieldMeta> getFields() {
        return fields;
    }

    public void setFields(List<DwFieldMeta> fields) {
        this.fields = fields == null ? new ArrayList<>() : fields;
    }
}
