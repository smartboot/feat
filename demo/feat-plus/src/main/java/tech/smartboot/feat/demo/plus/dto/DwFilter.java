package tech.smartboot.feat.demo.plus.dto;

import java.util.ArrayList;
import java.util.List;

/**
 * 列表筛选栏的一个筛选条件。key 与数据源约定：值由 DataSource 的 list 解释，
 * 语义上约定 input 为模糊匹配、select 为精确匹配。
 */
public class DwFilter {
    private String key;
    private String label;
    /** input | select */
    private String widget = "input";
    private String placeholder = "";
    private List<DwOption> options = new ArrayList<>();

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

    public String getWidget() {
        return widget;
    }

    public void setWidget(String widget) {
        this.widget = widget;
    }

    public String getPlaceholder() {
        return placeholder;
    }

    public void setPlaceholder(String placeholder) {
        this.placeholder = placeholder;
    }

    public List<DwOption> getOptions() {
        return options;
    }

    public void setOptions(List<DwOption> options) {
        this.options = options == null ? new ArrayList<>() : options;
    }
}
