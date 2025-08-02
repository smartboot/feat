/*
 *  Copyright (C) [2024] smartboot [zhengjunweimail@163.com]
 *
 *  企业用户未经smartboot组织特别许可，需遵循AGPL-3.0开源协议合理合法使用本项目。
 *
 *   Enterprise users are required to use this project reasonably
 *   and legally in accordance with the AGPL-3.0 open source agreement
 *  without special permission from the smartboot organization.
 */

package tech.smartboot.feat.cloud.aot.controller;

import javax.lang.model.type.DeclaredType;
import javax.lang.model.type.TypeMirror;
import java.io.IOException;
import java.io.PrintWriter;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * @author 三刀 zhengjunweimail@163.com
 * @version v1.0 5/13/25
 */
final class ListSerializer extends AbstractSerializer {

    public ListSerializer(JsonSerializer jsonSerializer) {
        super(jsonSerializer);
    }

    public void serialize(TypeMirror se, String obj, int i, DeclaredType parent) throws IOException {
        PrintWriter printWriter = jsonSerializer.getPrintWriter();
        if (parent == null) {
            printWriter.append(JsonSerializer.headBlank(i)).println("if (" + obj + " == null) {");
            printWriter.append(JsonSerializer.headBlank(i + 1));
            jsonSerializer.toBytesPool("null");
            printWriter.append(JsonSerializer.headBlank(i)).println("} else if (" + obj + ".isEmpty()) {");
        } else {
            printWriter.append(JsonSerializer.headBlank(i)).println("if (" + obj + ".isEmpty()) {");
        }

        printWriter.append(JsonSerializer.headBlank(i + 1));
        jsonSerializer.toBytesPool("[]");
        printWriter.append(JsonSerializer.headBlank(i)).println("} else {");
        //List未指定泛型,直接移交给fastjson处理
        if (((DeclaredType) se).getTypeArguments().isEmpty()) {
            printWriter.println(JsonSerializer.headBlank(i + 1) + "os.write(JSON.toJSONBytes(" + obj + "));");
            printWriter.append(JsonSerializer.headBlank(i)).println("}");
            return;
        }

        TypeMirror type = ((DeclaredType) se).getTypeArguments().get(0);
        printWriter.append(JsonSerializer.headBlank(i + 1)).println("os.write('[');");

        //获取泛型参数
        if (parent != null) {
            List<? extends TypeMirror> typeKey = ((DeclaredType) (parent.asElement().asType())).getTypeArguments();
            List<? extends TypeMirror> typeArgs = parent.getTypeArguments();
            Map<TypeMirror, TypeMirror> typeMap0 = new HashMap<>();
            for (int z = 0; z < typeArgs.size(); z++) {
                typeMap0.put(typeKey.get(z), typeArgs.get(z));
            }
            if (typeMap0.containsKey(type)) {
                type = typeMap0.get(type);
            }
        }
        printWriter.append(JsonSerializer.headBlank(i + 1)).println("boolean first" + i + " = true;");
        printWriter.append(JsonSerializer.headBlank(i + 1)).println("for (" + type + " p" + i + " : " + obj + " ) {");
        printWriter.append(JsonSerializer.headBlank(i + 2)).println("if (first" + i + ") {");
        printWriter.append(JsonSerializer.headBlank(i + 3)).println("first" + i + " = false;");
        printWriter.append(JsonSerializer.headBlank(i + 2)).println("} else {");
        printWriter.append(JsonSerializer.headBlank(i + 3)).println("os.write(',');");
        printWriter.append(JsonSerializer.headBlank(i + 2)).println("}");
        if (String.class.getName().equals(type.toString())) {
            printWriter.append(JsonSerializer.headBlank(i + 2)).println("os.write('\"');");
            printWriter.append(JsonSerializer.headBlank(i + 2)).println("os.write(p" + i + ".getBytes());");
            printWriter.append(JsonSerializer.headBlank(i + 2)).println("os.write('\"');");
        } else {
            jsonSerializer.serialize(type, "p" + i, i + 2, parent);
        }

        printWriter.append(JsonSerializer.headBlank(i + 1)).println("}");
        printWriter.append(JsonSerializer.headBlank(i + 1)).println("os.write(']');");

        printWriter.append(JsonSerializer.headBlank(i)).println("}");
    }
}
