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
 * MCP工具注解
 * 
 * 用于定义MCP工具，对应MCP协议中tools/list操作的工具定义。
 * 该注解支持定义工具的名称和描述信息，标记的方法将作为MCP工具对外提供服务。
 * 工具具有可执行性，可以与外部系统进行交互，是MCP协议的重要组成部分。
 * 有关MCP协议工具相关操作的详细信息，请参考：https://modelcontextprotocol.io/specification#tool
 *
 * @author 三刀
 * @version v1.0 7/21/25
 */
@Target({ElementType.METHOD})
@Retention(RetentionPolicy.SOURCE)
@Documented
public @interface Tool {
    /**
     * 工具名称，必须唯一
     * 对应MCP协议中工具的name字段
     * 如果未指定，则默认使用方法名
     */
    String name() default "";

    /**
     * 工具描述信息
     * 对应MCP协议中工具的description字段
     * 默认值：空字符串
     */
    String description() default "";
}