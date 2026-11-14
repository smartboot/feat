package tech.smartboot.feat.demo.plus.dto;

import java.util.ArrayList;
import java.util.List;

/**
 * 表单的一个字段描述。控件枚举与前端渲染器一一对应：
 * input | password | textarea | select | switch | span。
 * <p>
 * {@code immutable} 表示「创建可填、编辑只读」——角色编码这类创建后不可改的字段。
 * {@code pattern} 是 Java 与 JS 都能执行的正则源串，两端各自校验，前端拦体验、后端拦脏数据。
 * switch 的值统一用 "1" / "0" 字符串：动态层的所有值都是字符串，避免类型漂移。
 */
public class DwFormField {
    private String key;
    private String label;
    private String widget = "input";
    private boolean required;
    private boolean immutable;
    private String pattern = "";
    private String patternError = "";
    private String placeholder = "";
    private String defaultValue = "";
    private List<DwOption> options = new ArrayList<>();
    /** 1 = 半宽（两列布局的半边），2 = 全宽。 */
    private int span = 1;

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

    public boolean isRequired() {
        return required;
    }

    public void setRequired(boolean required) {
        this.required = required;
    }

    public boolean isImmutable() {
        return immutable;
    }

    public void setImmutable(boolean immutable) {
        this.immutable = immutable;
    }

    public String getPattern() {
        return pattern;
    }

    public void setPattern(String pattern) {
        this.pattern = pattern;
    }

    public String getPatternError() {
        return patternError;
    }

    public void setPatternError(String patternError) {
        this.patternError = patternError;
    }

    public String getPlaceholder() {
        return placeholder;
    }

    public void setPlaceholder(String placeholder) {
        this.placeholder = placeholder;
    }

    public String getDefaultValue() {
        return defaultValue;
    }

    public void setDefaultValue(String defaultValue) {
        this.defaultValue = defaultValue;
    }

    public List<DwOption> getOptions() {
        return options;
    }

    public void setOptions(List<DwOption> options) {
        this.options = options == null ? new ArrayList<>() : options;
    }

    public int getSpan() {
        return span;
    }

    public void setSpan(int span) {
        this.span = span;
    }
}
