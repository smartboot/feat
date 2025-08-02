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

/**
 * @author 三刀
 * @version v1.0 8/2/25
 */
public class MapSerializer extends AbstractSerializer {
    public MapSerializer(JsonSerializer jsonSerializer) {
        super(jsonSerializer);
    }

    @Override
    public void serialize(TypeMirror se, String obj, int i, DeclaredType parent) throws IOException {
        PrintWriter printWriter = jsonSerializer.getPrintWriter();
        printWriter.append(JsonSerializer.headBlank(i)).println("if (" + obj + " == null) {");
        printWriter.append(JsonSerializer.headBlank(i + 1));
        jsonSerializer.toBytesPool("null");
        printWriter.append(JsonSerializer.headBlank(i)).println("} else if (" + obj + ".isEmpty()) {");
        printWriter.append(JsonSerializer.headBlank(i + 1));
        jsonSerializer.toBytesPool("{}");
        printWriter.append(JsonSerializer.headBlank(i)).println("} else {");
        printWriter.append(JsonSerializer.headBlank(i + 1)).println("os.write(new JSONObject(" + obj + ").toString().getBytes());");
        printWriter.append(JsonSerializer.headBlank(i)).println("}");

    }
}
