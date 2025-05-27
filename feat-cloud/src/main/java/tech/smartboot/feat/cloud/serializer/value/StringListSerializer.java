/*
 *  Copyright (C) [2024] smartboot [zhengjunweimail@163.com]
 *
 *  企业用户未经smartboot组织特别许可，需遵循AGPL-3.0开源协议合理合法使用本项目。
 *
 *   Enterprise users are required to use this project reasonably
 *   and legally in accordance with the AGPL-3.0 open source agreement
 *  without special permission from the smartboot organization.
 */

package tech.smartboot.feat.cloud.serializer.value;

import com.alibaba.fastjson2.JSONArray;
import tech.smartboot.feat.core.common.exception.FeatException;

import javax.lang.model.element.Element;

/**
 * @author 三刀
 * @version v1.0 5/27/25
 */
class StringListSerializer extends AbstractSerializer {
    @Override
    public String serialize(Element field, Object paramValue) {
        JSONArray array = (JSONArray) paramValue;
        StringBuilder stringValue = new StringBuilder("java.util.Arrays.asList(");
        for (int i = 0; i < array.size(); i++) {
            if (i != 0) {
                stringValue.append(", ");
            }
            Object o = array.get(i);
            if (!(o instanceof String)) {
                throw new FeatException("compiler err: invalid value [ " + o + " ] for field[ " + field.getSimpleName() + " ] in class[ " + field.getEnclosingElement() + " ]");
            }
            stringValue.append(toString(o.toString()));
        }
        stringValue.append(")");
        return stringValue.toString();
    }
}
