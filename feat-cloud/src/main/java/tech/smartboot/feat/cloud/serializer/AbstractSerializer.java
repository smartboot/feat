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
import tech.smartboot.feat.core.common.FeatUtils;

import javax.lang.model.element.Element;
import javax.lang.model.type.DeclaredType;
import javax.lang.model.type.TypeMirror;
import java.io.IOException;

public abstract class AbstractSerializer {
    public void serialize(Element se, String obj, int deep) {
        throw new UnsupportedOperationException();
    }

    public void serialize(TypeMirror se, String obj, int deep, DeclaredType parent) throws IOException {
        throw new UnsupportedOperationException();
    }

    protected JsonSerializer jsonSerializer;

    public AbstractSerializer(JsonSerializer jsonSerializer) {
        this.jsonSerializer = jsonSerializer;
    }

    protected final String getFieldName(Element se) {
        String fieldName = se.getSimpleName().toString();
        JSONField jsonField = se.getAnnotation(JSONField.class);
        if (jsonField != null && FeatUtils.isNotBlank(jsonField.name())) {
            fieldName = jsonField.name();
        }
        return fieldName;
    }

}
