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

import javax.lang.model.element.Element;
import java.io.PrintWriter;
import java.util.Map;

public class BooleanSerializer extends JsonFieldSerializer {

    @Override
    public void serialize(PrintWriter printWriter, Element se, String obj, int deep, Map<String, String> byteCache) {
        String fieldName = getFieldName(se);
        printWriter.append(headBlank(deep) + "if (" + obj + ".is").append(se.getSimpleName().toString().substring(0, 1).toUpperCase()).append(se.getSimpleName().toString().substring(1)).println("()) {");
        printWriter.append(headBlank(deep + 1));
        toBytesPool(printWriter, byteCache, "\"" + fieldName + "\":true");

        printWriter.println(headBlank(deep) + "} else {");
        printWriter.append(headBlank(deep + 1));
        toBytesPool(printWriter, byteCache, "\"" + fieldName + "\":false");

        printWriter.println(headBlank(deep) + "}");
    }
}