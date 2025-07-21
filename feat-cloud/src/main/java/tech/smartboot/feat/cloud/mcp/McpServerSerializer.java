/*
 *  Copyright (C) [2024] smartboot [zhengjunweimail@163.com]
 *
 *  企业用户未经smartboot组织特别许可，需遵循AGPL-3.0开源协议合理合法使用本项目。
 *
 *   Enterprise users are required to use this project reasonably
 *   and legally in accordance with the AGPL-3.0 open source agreement
 *  without special permission from the smartboot organization.
 */

package tech.smartboot.feat.cloud.mcp;

import tech.smartboot.feat.cloud.annotation.mcp.Tool;

import javax.lang.model.element.Element;
import java.io.PrintWriter;

/**
 * @author 三刀
 * @version v1.0 7/20/25
 */
public class McpServerSerializer {
    public void serialize(PrintWriter printWriter, Element element) {
        element.getEnclosedElements().stream()
                .filter(e -> e.getAnnotation(Tool.class) != null)
                .forEach(field -> {

                });
    }
}
