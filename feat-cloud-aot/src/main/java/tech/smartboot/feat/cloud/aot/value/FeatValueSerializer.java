/*
 *  Copyright (C) [2024] smartboot [zhengjunweimail@163.com]
 *
 *  企业用户未经smartboot组织特别许可，需遵循AGPL-3.0开源协议合理合法使用本项目。
 *
 *   Enterprise users are required to use this project reasonably
 *   and legally in accordance with the AGPL-3.0 open source agreement
 *  without special permission from the smartboot organization.
 */

package tech.smartboot.feat.cloud.aot.value;

import tech.smartboot.feat.core.common.exception.FeatException;

import javax.lang.model.element.Element;
import java.util.HashMap;
import java.util.Map;

/**
 * @author 三刀
 * @version v1.0 7/23/25
 */
public class FeatValueSerializer {
    private static final Map<String, AbstractSerializer> serializers = new HashMap<>();

    static {
        serializers.put(int.class.getName(), new IntegerSerializer());
        serializers.put(String.class.getName(), new StringValueSerializer());
        serializers.put("int[]", new IntegerArraySerializer());
        serializers.put("java.util.List<java.lang.Integer>", new IntegerListSerializer());
        serializers.put("java.lang.String[]", new StringArraySerializer());
        serializers.put("java.util.List<java.lang.String>", new StringListSerializer());
    }

    public static String serialize(Element field, Object paramValue) {
        String fieldType = field.asType().toString();
        AbstractSerializer serializer = serializers.get(fieldType);
        if (serializer == null) {
            throw new FeatException("compiler err: unsupported type [ " + fieldType + " ] for field[ " + field.getSimpleName() + " ] in class[ " + field.getEnclosingElement() + " ]");
        }
        return serializer.serialize(field, paramValue);
    }
}
