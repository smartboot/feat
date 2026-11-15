package tech.smartboot.feat.demo.plus.dto;

import java.util.HashMap;
import java.util.Map;

/**
 * 动态表单的保存请求：id 为 null 表示新建，否则按 id 更新。
 * <p>
 * values 的 key 是表单字段 key（配置已校验命中数据源的可写字段），值统一字符串。
 * 「哪些字段可以写」由页面配置的 writeMode 决定（DwController 保存前清洗），
 * 数据源只负责按 key 取自己认识的字段。values 恒为可变 Map——保存通道会往里回填旧值。
 */
public class DwSaveRequest {
    private Long id;
    private Map<String, String> values = new HashMap<>();

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public Map<String, String> getValues() {
        return values;
    }

    public void setValues(Map<String, String> values) {
        this.values = values == null ? new HashMap<String, String>() : values;
    }
}
