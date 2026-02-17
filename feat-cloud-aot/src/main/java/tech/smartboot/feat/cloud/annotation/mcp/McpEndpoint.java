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
 * MCP服务端点注解
 * <p>
 * 用于定义MCP服务的端点配置，包括服务基本信息和各类MCP操作的端点地址。
 * 该注解定义了MCP服务的核心配置信息，包括服务名称、标题、版本以及各种MCP操作的端点。
 *
 * @author 三刀
 * @version v1.0 7/22/25
 * @see <a href="https://modelcontextprotocol.io/specification#endpoint-configuration">MCP Endpoint Configuration</a>
 */
@Target({ElementType.TYPE})
@Retention(RetentionPolicy.SOURCE)
@Documented
public @interface McpEndpoint {

    /**
     * MCP服务名称
     * 对应MCP协议中服务的name字段
     * 默认值："feat-mcp-server"
     */
    String name() default "feat-mcp-server";


    /**
     * MCP服务标题
     * 对应MCP协议中服务的title字段
     * 默认值："Feat MCP Server"
     */
    String title() default "Feat MCP Server";


    /**
     * MCP服务版本
     * 对应MCP协议中服务的version字段
     * 默认值：Feat.VERSION
     */
    String version() default Feat.VERSION;

    /**
     * SSE端点地址
     * 用于建立SSE连接的端点URL路径
     * 对应MCP协议中的SSE通信机制
     *
     * @deprecated MCP 官方不推荐使用
     */
    @Deprecated
    String sseEndpoint();

    /**
     * SSE消息端点地址
     * 用于发送SSE消息的端点URL路径
     * 对应MCP协议中的SSE消息传递机制
     *
     * @deprecated MCP 官方不推荐使用
     */
    @Deprecated
    String sseMessageEndpoint();


    /**
     * 流式传输端点地址
     * 用于支持流式数据传输的端点URL路径
     * 对应MCP协议中的流式传输机制
     */
    String streamableEndpoint();

    /**
     * 资源功能开关
     * 控制是否启用MCP资源(resources/list)功能
     * 默认值：true(启用)
     *
     * @see <a href="https://modelcontextprotocol.io/specification#resources">MCP Resources</a>
     */
    boolean resourceEnable() default true;

    /**
     * 工具功能开关
     * 控制是否启用MCP工具(tools/list, tools/call)功能
     * 默认值：true(启用)
     *
     * @see <a href="https://modelcontextprotocol.io/specification#tools">MCP Tools</a>
     */
    boolean toolEnable() default true;


    /**
     * 提示词功能开关
     * 控制是否启用MCP提示词(prompts/list)功能
     * 默认值：true(启用)
     *
     * @see <a href="https://modelcontextprotocol.io/specification#prompts">MCP Prompts</a>
     */
    boolean promptsEnable() default true;


    /**
     * 日志功能开关
     * 控制是否启用MCP日志(logging)功能
     * 默认值：true(启用)
     *
     * @see <a href="https://modelcontextprotocol.io/specification#logging">MCP Logging</a>
     */
    boolean loggingEnable() default true;


}