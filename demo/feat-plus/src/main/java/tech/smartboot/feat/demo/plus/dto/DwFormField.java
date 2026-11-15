package tech.smartboot.feat.demo.plus.dto;

import java.util.ArrayList;
import java.util.List;

/**
 * 表单的一个字段描述。控件枚举与前端渲染器一一对应：
 * input | password | textarea | select | switch | span。
 * <p>
 * <b>写入策略 {@code writeMode} 是唯一真相</b>，取值：
 * <ul>
 *   <li>{@code always}：新建、编辑都可写；</li>
 *   <li>{@code createOnly}：新建可写，编辑态只读——角色编码这类「一次定死」的字段；</li>
 *   <li>{@code readonly}：始终只读，只展示当前值。</li>
 * </ul>
 * 它不只是渲染开关：{@code DwController} 保存时会按它清洗入参，前端 disabled 只是体验，
 * 服务端才是「改不了」的保证。{@code immutable} 是旧配置里的等价表达（true = createOnly），
 * 保存时由 {@code PageService} 归一成 writeMode，读旧数据自动兼容。
 * <p>
 * {@code pattern} 是 Java 与 JS 都能执行的正则源串，两端各自校验，前端拦体验、后端拦脏数据。
 * switch 的值统一用 "1" / "0" 字符串：动态层的所有值都是字符串，避免类型漂移。
 */
public class DwFormField {
    private String key;
    private String label;
    private String widget = "input";
    private boolean required;
    /** 写入策略：always | createOnly | readonly。空值表示「配置里没写」，由 getter 按 immutable 推导。 */
    private String writeMode = "";
    /** 兼容字段：true 等价于 writeMode=createOnly，保存时由 writeMode 反推。 */
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

    /**
     * 空值不能默认返回 always：老配置只有 immutable，若这里默认 always，序列化出去就把
     * 「创建后锁定」抹掉了。缺省时按 immutable 推导，读旧数据与读新数据得到同一种语义。
     */
    public String getWriteMode() {
        if (writeMode == null || writeMode.isEmpty()) {
            return immutable ? "createOnly" : "always";
        }
        return writeMode;
    }

    public void setWriteMode(String writeMode) {
        this.writeMode = writeMode;
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
