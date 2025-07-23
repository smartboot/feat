/*
 *  Copyright (C) [2024] smartboot [zhengjunweimail@163.com]
 *
 *  企业用户未经smartboot组织特别许可，需遵循AGPL-3.0开源协议合理合法使用本项目。
 *
 *   Enterprise users are required to use this project reasonably
 *   and legally in accordance with the AGPL-3.0 open source agreement
 *  without special permission from the smartboot organization.
 */

package tech.smartboot.feat.cloud.annotation.mcp;

import tech.smartboot.feat.Feat;

import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * @author 三刀
 * @version v1.0 7/22/25
 */
@Target({ElementType.TYPE})
@Retention(RetentionPolicy.SOURCE)
@Documented
public @interface McpEndpoint {
    String name() default "feat-mcp-server";

    String title() default "Feat MCP Server";

    String version() default Feat.VERSION;

    String mcpSseEndpoint();

    String mcpSseMessageEndpoint();

    String mcpStreamableEndpoint();
}
