package tech.smartboot.feat.demo.plus.dto;

import java.util.Collections;
import java.util.Map;

/**
 * 动态表单的保存请求：id 为 null 表示新建，否则按 id 更新。
 * <p>
 * values 的 key 是表单字段 key（配置已校验命中数据源的可写字段），值统一字符串。
 * 「哪些字段可以写」的约束在 DataSource 实现里收口：不在声明字段里的键直接忽略。
 */
public class DwSaveRequest {
    private Long id;
    private Map<String, String> values = Collections.emptyMap();

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
        this.values = values == null ? Collections.<String, String>emptyMap() : values;
    }
}
