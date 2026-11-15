package tech.smartboot.feat.demo.plus.dw.dto;

import java.util.ArrayList;
import java.util.List;

/**
 * 一个列表筛选。控件枚举与前端渲染器一一对应：input | select | dateRange。
 * <p>
 * <b>值怎么传。</b> 所有筛选键值都进同一个 {@code filter} JSON 串（{@code PageDataQuery.filters}），
 * 一个键一个字符串。日期范围因此占<b>两个键</b>：起止分别用 {@code key} 与 {@code endKey}，
 * 前端产出 {@code {"from":"2026-08-01","to":"2026-08-30"}}。
 * <p>
 * 不用「一个键塞逗号拼接」的理由：数据源读的是命名参数（{@code filters.get("from")}），
 * 拼接值让每个数据源都要写一遍 split，且空值的两种写法（{@code "~"} / {@code ""}）无法自证。
 * 两个键与手写接口的查询参数形状完全一致，迁移时不用改写业务代码。
 * <p>
 * <b>选项从哪来。</b> 静态枚举填 {@code options}；随数据变化变化的选项（角色、部门、操作人）
 * 填 {@code optionsApi} —— 站内 /api/ 路径，返回对象数组，用 {@code optionValueField} /
 * {@code optionLabelField} 指定取哪两个字段。只允许站内相对路径，与 {@code DwDataApi} 同一条边界：
 * 配置能改渲染，不能变成外链发起方或内网探测的跳板。
 * <p>
 * 远程选项<b>不缓存进 sys_page 配置</b>：存的是「去哪取」，不是取回来的结果，
 * 所以角色改名、新增部门之后不需要重新保存页面配置。
 */
public class DwFilter {
    private String key;
    private String label;
    /** input | select | dateRange */
    private String widget = "input";
    private String placeholder = "";
    /** dateRange 的结束键；其他控件留空。 */
    private String endKey = "";
    /** 静态选项。 */
    private List<DwOption> options = new ArrayList<>();
    /** 远程选项路径，如 /api/activities/operators；填了它就不用填 options。 */
    private String optionsApi = "";
    /** 远程选项的取值字段，如 id；留空按 value。 */
    private String optionValueField = "";
    /** 远程选项的显示字段，如 name；留空按 label。 */
    private String optionLabelField = "";

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

    public String getEndKey() {
        return endKey;
    }

    public void setEndKey(String endKey) {
        this.endKey = endKey == null ? "" : endKey;
    }

    public List<DwOption> getOptions() {
        return options;
    }

    public void setOptions(List<DwOption> options) {
        this.options = options == null ? new ArrayList<>() : options;
    }

    public String getOptionsApi() {
        return optionsApi;
    }

    public void setOptionsApi(String optionsApi) {
        this.optionsApi = optionsApi == null ? "" : optionsApi;
    }

    public String getOptionValueField() {
        return optionValueField;
    }

    public void setOptionValueField(String optionValueField) {
        this.optionValueField = optionValueField == null ? "" : optionValueField;
    }

    public String getOptionLabelField() {
        return optionLabelField;
    }

    public void setOptionLabelField(String optionLabelField) {
        this.optionLabelField = optionLabelField == null ? "" : optionLabelField;
    }
}
