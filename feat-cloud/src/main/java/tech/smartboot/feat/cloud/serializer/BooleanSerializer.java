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

class BooleanSerializer extends AbstractSerializer {

    public BooleanSerializer(JsonSerializer jsonSerializer) {
        super(jsonSerializer);
    }

    @Override
    public void serialize(Element se, String obj, int deep) {
        PrintWriter printWriter = jsonSerializer.getPrintWriter();
        String fieldName = getFieldName(se);
        printWriter.append(JsonSerializer.headBlank(deep) + "if (" + obj + ".is").append(se.getSimpleName().toString().substring(0, 1).toUpperCase()).append(se.getSimpleName().toString().substring(1)).println("()) {");
        printWriter.append(JsonSerializer.headBlank(deep + 1));
        jsonSerializer.toBytesPool("\"" + fieldName + "\":true");

        printWriter.println(JsonSerializer.headBlank(deep) + "} else {");
        printWriter.append(JsonSerializer.headBlank(deep + 1));
        jsonSerializer.toBytesPool("\"" + fieldName + "\":false");

        printWriter.println(JsonSerializer.headBlank(deep) + "}");
    }
}