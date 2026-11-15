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
    /**
     * 值 → 文案（可带样式变体）的映射表，任何格式都可以配。
     * <p>
     * 它管的是「翻译」，不是「呈现」：{@code code} 列想显示「超管」而不是 {@code ROLE_ADMIN}，
     * 加一条映射即可，不必为了拿到映射能力把格式改成彩色标签、让整列视觉跳出来。
     * 命中映射时不再套 {@link #valueFormat}——已经翻译成给人看的字了，再格式化没有意义。
     */
    private List<DwOption> options = new ArrayList<>();
    /**
     * 值的排版方式：空串原样，{@code date} → 2026-11-15，{@code datetime} → 2026-11-15 18:53，
     * {@code thousands} → 1,234,567。
     * <p>
     * 用来兜住「数据源直接给原始值」的情况（第三方接口返回 ISO 串或时间戳）。
     * 解析不出来就原样显示，不做静默清空。
     */
    private String valueFormat = "";

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

    public String getValueFormat() {
        return valueFormat;
    }

    public void setValueFormat(String valueFormat) {
        this.valueFormat = valueFormat == null ? "" : valueFormat;
    }
}
