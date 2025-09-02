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

import tech.smartboot.feat.ai.mcp.enums.PromptType;
import tech.smartboot.feat.ai.mcp.enums.RoleEnum;

import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * MCP提示词注解
 * 
 * 用于定义MCP提示词资源，对应MCP协议中prompts/list操作的提示词定义。
 * 该注解支持定义提示词的名称、描述、类型、角色和MIME类型等信息。
 * 有关MCP协议提示词相关操作的详细信息，请参考：https://modelcontextprotocol.io/specification#prompt
 *
 * @author 三刀
 * @version v1.0 7/21/25
 * @see <a href="https://modelcontextprotocol.io/specification#prompts">MCP Prompts</a>
 * @see <a href="https://modelcontextprotocol.io/specification#prompt">MCP Prompt Object</a>
 */
@Target({ElementType.METHOD})
@Retention(RetentionPolicy.SOURCE)
@Documented
public @interface Prompt {
    /**
     * 提示词名称，必须唯一
     * 对应MCP协议中提示词的name字段，用于唯一标识一个提示词资源
     * 
     * @return 提示词名称
     */
    String name();

    /**
     * 提示词描述信息
     * 对应MCP协议中提示词的description字段，用于向客户端说明提示词的用途
     * 默认值：空字符串
     * 
     * @return 提示词描述文本
     */
    String description() default "";

    /**
     * 提示词类型
     * 对应MCP协议中提示词的type字段，定义提示词的内容类型。
     * 
     * @return 提示词类型枚举值
     * @see PromptType
     */
    PromptType type();

    /**
     * 提示词角色
     * 对应MCP协议中提示词的role字段，默认为User角色。该字段定义了提示词内容应该由谁（用户或AI）来处理
     * 
     * @return 提示词角色枚举值
     * @see RoleEnum
     */
    RoleEnum role() default RoleEnum.User;

    /**
     * 提示词内容的MIME类型
     * 对应MCP协议中提示词的mimeType字段，用于指定提示词内容的格式类型
     * 默认值：空字符串
     * 
     * @return MIME类型字符串
     */
    String mimeType() default "";
}