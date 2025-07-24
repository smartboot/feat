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

import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * MCP工具参数注解
 * 
 * 用于定义MCP工具的参数信息，对应MCP协议中tools/call操作的参数定义。
 * 该注解定义了参数的必要性及描述信息，用于生成JSON Schema。
 * 有关MCP协议tools相关操作的详细信息，请参考：https://modelcontextprotocol.io/specification#tool_call
 *
 * @author 三刀
 * @version v1.0 7/21/25
 * @see <a href="https://modelcontextprotocol.io/specification#tool_call">MCP Tool Call</a>
 */
@Target({ElementType.PARAMETER})
@Retention(RetentionPolicy.SOURCE)
@Documented
public @interface Param {
    /**
     * 参数是否必需
     * 对应JSON Schema中的required字段
     * 在MCP协议中，此属性用于确定tools/call时参数是否必须提供
     * @see <a href="https://json-schema.org/draft/2020-12/json-schema-validation.html#rfc.section.6.5.3">JSON Schema Required Fields</a>
     */
    boolean required();

    /**
     * 参数描述信息
     * 对应JSON Schema中的description字段
     * 在MCP协议中，此属性用于向客户端描述参数的用途
     * 默认值：空字符串
     * 
     * @return 参数描述文本
     */
    String description() default "";
}