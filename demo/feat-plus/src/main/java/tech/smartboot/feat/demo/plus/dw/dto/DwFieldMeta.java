package tech.smartboot.feat.demo.plus.dw.dto;

/**
 * 数据源声明的一个可用字段：key、给人看的名称、值类型（string / number / list）、
 * 是否允许出现在表单里（只读展示字段不可写）。
 * <p>
 * 这份声明是「防配置漂移」的锚点：页面配置里的列 / 筛选 / 表单字段 key 必须命中它，
 * 数据源改了字段名，保存配置时会立即报错，而不是渲染时静默空列。
 */
public class DwFieldMeta {
    private String key;
    private String label;
    /** string | number | list */
    private String type;
    private boolean writable;

    public DwFieldMeta() {
    }

    public DwFieldMeta(String key, String label, String type, boolean writable) {
        this.key = key;
        this.label = label;
        this.type = type;
        this.writable = writable;
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

    public String getType() {
        return type;
    }

    public void setType(String type) {
        this.type = type;
    }

    public boolean isWritable() {
        return writable;
    }

    public void setWritable(boolean writable) {
        this.writable = writable;
    }
}
