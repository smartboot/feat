/*
 *  Copyright (C) [2024] smartboot [zhengjunweimail@163.com]
 *
 *  企业用户未经smartboot组织特别许可，需遵循AGPL-3.0开源协议合理合法使用本项目。
 *
 *   Enterprise users are required to use this project reasonably
 *   and legally in accordance with the AGPL-3.0 open source agreement
 *  without special permission from the smartboot organization.
 */

package tech.smartboot.feat.cloud.serializer;

import com.alibaba.fastjson2.annotation.JSONField;
import tech.smartboot.feat.core.common.utils.StringUtils;

import javax.lang.model.element.Element;
import java.io.IOException;
import java.io.PrintWriter;
import java.util.Map;

public abstract class JsonFieldSerializer {
    public abstract void serialize(PrintWriter printWriter, Element se, String obj, int deep, Map<String, String> byteCache) throws IOException;

    protected String getFieldName(Element se) {
        String fieldName = se.getSimpleName().toString();
        JSONField jsonField = se.getAnnotation(JSONField.class);
        if (jsonField != null && StringUtils.isNotBlank(jsonField.name())) {
            fieldName = jsonField.name();
        }
        return fieldName;
    }

    protected String headBlank(int i) {
        StringBuilder sb = new StringBuilder("\t\t");
        do {
            sb.append("\t");
        } while (i-- > 0);
        return sb.toString();
    }

    protected void toBytesPool(PrintWriter printWriter, Map<String, String> map, String value) {
        String key = ("b_" + value.hashCode()).replace("-", "$");
        map.put(key, "private static final byte[] " + key + " = " + toBytesStr(value) + ";");
        printWriter.append("os.write(").append(key).println(");");
    }

    protected String toBytesStr(String str) {
        StringBuilder s = new StringBuilder("{");

        for (int i = 0; i < str.length(); i++) {
            if (i > 0) {
                s.append(", ");
            }
            s.append('\'').append(str.charAt(i)).append('\'');
        }
        return s + "}";
    }
}
