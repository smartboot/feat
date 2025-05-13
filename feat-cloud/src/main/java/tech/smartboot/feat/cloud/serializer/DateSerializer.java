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
import java.io.PrintWriter;
import java.util.Map;

public class DateSerializer extends JsonFieldSerializer {
    @Override
    public void serialize(PrintWriter printWriter, Element se, String obj, int deep, Map<String, String> byteCache) {
        String fieldName = getFieldName(se);
        JSONField jsonField = se.getAnnotation(JSONField.class);
        printWriter.append(headBlank(deep));
        toBytesPool(printWriter, byteCache, "\"" + fieldName + "\":");
        printWriter.append(headBlank(deep));
        printWriter.append("java.util.Date " + fieldName + " = ").append(obj).append(".get").append(se.getSimpleName().toString().substring(0, 1).toUpperCase()).append(se.getSimpleName().toString().substring(1)).println("();");
        printWriter.append(headBlank(deep));
        printWriter.println("if (" + fieldName + " != null) {");
        if (jsonField != null && StringUtils.isNotBlank(jsonField.format())) {
            printWriter.append(headBlank(deep + 1)).println("java.text.SimpleDateFormat sdf = new java.text.SimpleDateFormat(\"" + jsonField.format() + "\");");
            printWriter.append(headBlank(deep + 1)).println("os.write('\"');");
            printWriter.append(headBlank(deep + 1)).println("os.write(sdf.format(" + fieldName + ").getBytes());");
            printWriter.append(headBlank(deep + 1)).println("os.write('\"');");
        } else {
            printWriter.append(headBlank(deep + 1)).println("os.write(String.valueOf(" + fieldName + ".getTime()).getBytes());");
        }

//                printWriter.println("os.write('\"');");
        printWriter.append(headBlank(deep)).println("} else {");
        printWriter.append(headBlank(deep + 1));
        toBytesPool(printWriter, byteCache, "null");
        printWriter.append(headBlank(deep)).println("}");
    }
}