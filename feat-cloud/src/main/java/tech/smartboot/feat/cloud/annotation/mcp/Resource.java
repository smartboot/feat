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
 * 有关MCP协议资源相关操作的详细信息，请参考：https://modelcontextprotocol.io/specification#resource
 *
 * @author 三刀
 * @version v1.0 7/21/25
 */
@Target({ElementType.METHOD})
@Retention(RetentionPolicy.SOURCE)
@Documented
public @interface Resource {
    /**
     * 资源URI，必须唯一
     * 对应MCP协议中资源的uri字段，遵循URI规范
     */
    String uri();

    /**
     * 资源名称
     * 对应MCP协议中资源的name字段
     */
    String name();

    /**
     * 资源描述信息
     * 对应MCP协议中资源的description字段
     * 默认值：空字符串
     */
    String description() default "";

    /**
     * 资源类型标识
     * true表示文本资源，false表示二进制资源
     * 对应MCP协议中文本资源和二进制资源的不同处理机制
     */
    boolean isText() default true;

    /**
     * 资源内容的MIME类型
     * 对应MCP协议中资源的mimeType字段
     * 默认值：空字符串
     */
    String mimeType() default "";
}