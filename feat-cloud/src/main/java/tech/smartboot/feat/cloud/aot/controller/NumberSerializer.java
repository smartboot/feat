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

import javax.lang.model.element.Element;
import java.io.PrintWriter;

/**
 * @author 三刀 zhengjunweimail@163.com
 * @version v1.0 5/14/25
 */
final class NumberSerializer extends AbstractSerializer {

    public NumberSerializer(JsonSerializer jsonSerializer) {
        super(jsonSerializer);
    }

    @Override
    public void serialize(Element se, String obj, int deep) {
        PrintWriter printWriter = jsonSerializer.getPrintWriter();
        printWriter.append(JsonSerializer.headBlank(deep));
        jsonSerializer.toBytesPool("\"" + getFieldName(se) + "\":");
        printWriter.append(JsonSerializer.headBlank(deep));
        printWriter.append("writeNumber(os, ").append(obj).append(".get").append(se.getSimpleName().toString().substring(0, 1).toUpperCase()).append(se.getSimpleName().toString().substring(1)).println("());");
    }
}
