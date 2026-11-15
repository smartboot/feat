package tech.smartboot.feat.demo.plus.dw.dto;

import java.util.ArrayList;
import java.util.List;

/**
 * 列表的一列描述。key 必须命中数据源声明的字段，format 决定单元格的渲染方式。
 * <p>
 * {@code width} 存 Tailwind 宽度 class（如 "w-36"），空串表示自适应——
 * 渲染细节留在前端，配置只声明意图。
 */
public class DwColumn {
    private String key;
    private String label;
    /** text | muted | badge | tags | number */
    private String format = "text";
    private String width = "";
    /** badge / tags 格式的值 → 文案与变体映射；其他格式忽略。 */
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

    public String getFormat() {
        return format;
    }

    public void setFormat(String format) {
        this.format = format;
    }

    public String getWidth() {
        return width;
    }

    public void setWidth(String width) {
        this.width = width;
    }

    public List<DwOption> getOptions() {
        return options;
    }

    public void setOptions(List<DwOption> options) {
        this.options = options == null ? new ArrayList<>() : options;
    }
}
