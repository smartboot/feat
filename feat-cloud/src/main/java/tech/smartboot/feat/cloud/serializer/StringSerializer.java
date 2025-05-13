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

public class StringSerializer extends JsonFieldSerializer {

    @Override
    public void serialize(PrintWriter printWriter, Element se, String obj, int deep, Map<String, String> byteCache) {
        String fieldName = getFieldName(se);
        printWriter.append(headBlank(deep));
        toBytesPool(printWriter, byteCache, "\"" + fieldName + "\":");

        printWriter.append(headBlank(deep));
        printWriter.append("if (").append(obj).append(".get").append(se.getSimpleName().toString().substring(0, 1).toUpperCase()).append(se.getSimpleName().toString().substring(1)).println("()" + " != null) {");
        printWriter.append(headBlank(deep + 1));
        printWriter.println("os.write('\"');");
        printWriter.append(headBlank(deep + 1));
        printWriter.println("String s = " + obj + ".get" + se.getSimpleName().toString().substring(0, 1).toUpperCase() + se.getSimpleName().toString().substring(1) + "();");
        printWriter.append(headBlank(deep + 1));
        printWriter.println("writeJsonValue(os, s);");
        printWriter.append(headBlank(deep + 1));
        printWriter.println("os.write('\"');");
        printWriter.append(headBlank(deep));
        printWriter.println("} else {");
        printWriter.append(headBlank(deep + 1));
        toBytesPool(printWriter, byteCache, "null");
        printWriter.append(headBlank(deep)).println("}");
    }
}