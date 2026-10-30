/*
 *  Copyright (C) [2024] smartboot [zhengjunweimail@163.com]
 *
 *  企业用户未经smartboot组织特别许可，需遵循Apache-2.0开源协议合理合法使用本项目。
 *
 *   Enterprise users are required to use this project reasonably
 *   and legally in accordance with the Apache-2.0 open source agreement
 *  without special permission from the smartboot organization.
 */

package tech.smartboot.feat.ai.mcp.model.elicitation;

import com.alibaba.fastjson2.JSONArray;
import com.alibaba.fastjson2.JSONObject;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

/**
 * elicitation form 模式下的枚举属性 schema。
 * <p>
 * 2025-11-25 起支持四种形态：无标题单选、有标题单选（oneOf + const/title）、
 * 无标题多选（items.enum）、有标题多选（items.anyOf + const/title）。
 *
 * @author 三刀 zhengjunweimail@163.com
 * @version v1.0
 */
public class EnumSchema {
    private final JSONObject schema = new JSONObject();

    private EnumSchema(String type) {
        schema.put("type", type);
    }

    /**
     * 无标题单选，输出 type=string + enum。
     */
    public static EnumSchema single(String... values) {
        EnumSchema result = new EnumSchema("string");
        result.schema.put("enum", Arrays.asList(values));
        return result;
    }

    /**
     * 有标题单选，输出 type=string + oneOf[{const,title}]。
     */
    public static EnumSchema singleTitled(Option... options) {
        EnumSchema result = new EnumSchema("string");
        result.schema.put("oneOf", toJSONArray(options));
        return result;
    }

    /**
     * 无标题多选，输出 type=array + items{type,enum}。
     */
    public static EnumSchema multi(String... values) {
        EnumSchema result = new EnumSchema("array");
        JSONObject items = new JSONObject();
        items.put("type", "string");
        items.put("enum", Arrays.asList(values));
        result.schema.put("items", items);
        return result;
    }

    /**
     * 有标题多选，输出 type=array + items{anyOf[{const,title}]}。
     */
    public static EnumSchema multiTitled(Option... options) {
        EnumSchema result = new EnumSchema("array");
        JSONObject items = new JSONObject();
        items.put("anyOf", toJSONArray(options));
        result.schema.put("items", items);
        return result;
    }

    private static JSONArray toJSONArray(Option... options) {
        JSONArray array = new JSONArray();
        if (options != null) {
            for (Option option : options) {
                array.add(option.toJSON());
            }
        }
        return array;
    }

    public EnumSchema title(String title) {
        schema.put("title", title);
        return this;
    }

    public EnumSchema description(String description) {
        schema.put("description", description);
        return this;
    }

    public EnumSchema minItems(int minItems) {
        schema.put("minItems", minItems);
        return this;
    }

    public EnumSchema maxItems(int maxItems) {
        schema.put("maxItems", maxItems);
        return this;
    }

    /**
     * 默认值：单选传字符串，多选传集合。
     */
    public EnumSchema defaultValue(Object value) {
        schema.put("default", value);
        return this;
    }

    public JSONObject toJSON() {
        return schema;
    }

    /**
     * 带标题的枚举取值，对应 oneOf / anyOf 中的 {const, title}。
     */
    public static class Option {
        private final String value;
        private final String title;

        private Option(String value, String title) {
            this.value = value;
            this.title = title;
        }

        public static Option of(String value, String title) {
            return new Option(value, title);
        }

        public JSONObject toJSON() {
            JSONObject json = new JSONObject();
            json.put("const", value);
            json.put("title", title);
            return json;
        }
    }

    /**
     * 批量构造 Option，便于 singleTitled / multiTitled 使用。
     */
    public static List<Option> options(Option... options) {
        return options == null ? new ArrayList<Option>() : Arrays.asList(options);
    }
}
