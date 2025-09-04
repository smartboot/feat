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
 * MCP资源注解
 * 
 * 用于定义MCP资源，对应MCP协议中resources/list操作的资源定义。
 * 该注解支持定义资源的URI、名称、描述、类型（文本或二进制）和MIME类型等信息。
 * 文本资源和二进制资源在MCP协议中有不同的处理方式，需要明确区分。
 *
 * @author 三刀
 * @version v1.0 7/21/25
 * @see <a href="https://modelcontextprotocol.io/specification#resources">MCP Resources</a>
 */
@Target({ElementType.METHOD})
@Retention(RetentionPolicy.RUNTIME)
@Documented
public @interface Resource {
    /**
     * 资源URI
     * 在MCP协议中，该URI用于唯一标识和访问一个资源
     * 
     * @return 资源URI字符串
     */
    String uri();

    /**
     * 资源名称
     * 对应MCP协议中资源的name字段，用于向用户展示资源的可读名称
     * 
     * @return 资源名称
     */
    String name();

    /**
     * 资源描述信息
     * 对应MCP协议中资源的description字段，用于向客户端说明资源的用途和内容
     * 默认值：空字符串
     * 
     * @return 资源描述文本
     */
    String description() default "";

    /**
     * 资源类型标识
     * true表示文本资源，false表示二进制资源
     * 对应MCP协议中文本资源和二进制资源的不同处理机制，文本资源可以直接在客户端显示，而二进制资源可能需要特殊处理
     * 
     * @return 资源类型布尔值，true为文本资源，false为二进制资源
     */
    boolean isText() default true;

    /**
     * 资源内容的MIME类型
     * 对应MCP协议中资源的mimeType字段，用于指定资源内容的格式类型，帮助客户端正确处理资源
     * 默认值：空字符串
     * 
     * @return MIME类型字符串
     */
    String mimeType() default "";
}