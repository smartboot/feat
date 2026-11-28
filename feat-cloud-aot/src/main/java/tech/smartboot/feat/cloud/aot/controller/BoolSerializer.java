/*
 *  Copyright (C) [2024] smartboot [zhengjunweimail@163.com]
 *
 *  企业用户未经smartboot组织特别许可，需遵循Apache-2.0开源协议合理合法使用本项目。
 *
 *   Enterprise users are required to use this project reasonably
 *   and legally in accordance with the Apache-2.0 open source agreement
 *  without special permission from the smartboot organization.
 */

package tech.smartboot.feat.cloud.aot.controller;

import javax.lang.model.element.Element;
import java.io.PrintWriter;

final class BoolSerializer extends AbstractSerializer {

    public BoolSerializer(JsonSerializer jsonSerializer) {
        super(jsonSerializer);
    }

    static String getterName(String fieldName) {
        if (fieldName.length() > 2 && fieldName.startsWith("is")
                && Character.isUpperCase(fieldName.charAt(2))) {
            return fieldName;
        }
        return "is" + Character.toUpperCase(fieldName.charAt(0)) + fieldName.substring(1);
    }

    @Override
    public void serialize(Element se, String obj, int deep, boolean withComma) {
        PrintWriter printWriter = jsonSerializer.getPrintWriter();
        String fieldName = getFieldName(se);
        printWriter.append(JsonSerializer.headBlank(deep)).append("if (").append(obj).append(".").append(getterName(se.getSimpleName().toString())).println("()) {");
        printWriter.append(JsonSerializer.headBlank(deep + 1));
        jsonSerializer.toBytesPool("\"" + fieldName + "\":true", withComma);

        printWriter.println(JsonSerializer.headBlank(deep) + "} else {");
        printWriter.append(JsonSerializer.headBlank(deep + 1));
        jsonSerializer.toBytesPool("\"" + fieldName + "\":false", withComma);

        printWriter.println(JsonSerializer.headBlank(deep) + "}");
    }
}