package tech.smartboot.feat.demo.plus.dto;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import tech.smartboot.feat.demo.plus.common.DomainException;

/**
 * 动态表单的保存请求：id 为 null 表示新建，否则按 id 更新。
 * <p>
 * values 的 key 是表单字段 key（配置已校验命中数据源的可写字段）。<b>值不一定是字符串</b>：
 * 多选控件（{@code DwFormField.multiple}）提交的是数组，所以容器类型是 {@code Map<String, Object>}。
 * <p>
 * 取值一律走 {@link #text(String)} / {@link #list(String)}，不要直接对 {@code getValues()} 强转：
 * <ul>
 *   <li>{@code text} 撞上数组会抛异常，而不是静默返回空串——后者会让「保存后权限被清空」
 *       变成一个没有报错的线上事故；</li>
 *   <li>{@code list} 同时接受数组与单个字符串，前端版本不一致时不至于 500。</li>
 * </ul>
 * 「哪些字段可以写」由页面配置的 writeMode 决定（DwController 保存前清洗），
 * 数据源只负责按 key 取自己认识的字段。values 恒为可变 Map——保存通道会往里回填旧值。
 */
public class DwSaveRequest {
    private Long id;
    private Map<String, Object> values = new HashMap<>();

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public Map<String, Object> getValues() {
        return values;
    }

    public void setValues(Map<String, Object> values) {
        this.values = values == null ? new HashMap<String, Object>() : values;
    }

    /** 字段是否出现在本次提交里。区分「没传」与「传了空值」，回填逻辑靠它判断。 */
    public boolean has(String key) {
        return values.containsKey(key);
    }

    /** 标量取值。字段不存在或值为 null 时返回空串，与 {@code values.get(key)} 的历史语义一致。 */
    public String text(String key) {
        Object raw = values.get(key);
        if (raw == null) {
            return "";
        }
        if (raw instanceof Collection) {
            throw new DomainException("字段 " + key + " 是多值字段，请用 list() 取值");
        }
        return String.valueOf(raw).trim();
    }

    /**
     * 多值取值。数组按序收集，单个标量当作只有一项的数组——
     * 这让「多选字段只勾了一个」在前端序列化成字符串时也不会丢。
     */
    public List<String> list(String key) {
        Object raw = values.get(key);
        List<String> result = new ArrayList<>();
        if (raw == null) {
            return result;
        }
        if (raw instanceof Collection) {
            for (Object item : (Collection<?>) raw) {
                addIfPresent(result, item);
            }
            return result;
        }
        addIfPresent(result, raw);
        return result;
    }

    private static void addIfPresent(List<String> target, Object item) {
        if (item == null) {
            return;
        }
        String value = String.valueOf(item).trim();
        if (!value.isEmpty()) {
            target.add(value);
        }
    }
}
