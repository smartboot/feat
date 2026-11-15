package tech.smartboot.feat.demo.plus.dw.dto;

/**
 * 动态渲染的一个枚举选项：select 的 option、badge / tags 的值映射。
 * <p>
 * variant 对齐前端 shadcn Badge 的变体名（success / warning / destructive / secondary / outline），
 * 只影响展示颜色，不影响语义——「已启用 / 已停用」用哪个颜色是配置的事，控件不掺和。
 */
public class DwOption {
    private String value;
    private String label;
    private String variant = "secondary";

    public DwOption() {
    }

    public DwOption(String value, String label, String variant) {
        this.value = value;
        this.label = label;
        this.variant = variant;
    }

    public String getValue() {
        return value;
    }

    public void setValue(String value) {
        this.value = value;
    }

    public String getLabel() {
        return label;
    }

    public void setLabel(String label) {
        this.label = label;
    }

    public String getVariant() {
        return variant;
    }

    public void setVariant(String variant) {
        this.variant = variant;
    }
}
