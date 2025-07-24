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
 * 定义MCP（Management Control Plane）端点配置
 * 用于标识包含MCP服务配置的类
 *
 * @author 三刀
 * @version v1.0 7/22/25
 * <p>
 * name() - 服务名称，默认为"feat-mcp-server"
 * title() - 文档标题，默认为"Feat MCP Server"
 * version() - 版本号，默认使用Feat.VERSION
 * mcpSseEndpoint() - SSE端点路径，必须指定
 * mcpSseMessageEndpoint() - SSE消息端点路径，必须指定
 * mcpStreamableEndpoint() - 可流式传输的端点路径，必须指定
 * <p>
 * 以下选项用于启用/禁用不同功能模块：
 * resourceEnable() - 资源管理功能，默认启用
 * resourceChangeNotification() - 资源变更通知，默认启用
 * toolEnable() - 工具管理功能，默认启用
 * toolChangeNotification() - 工具变更通知，默认启用
 * promptEnable() - 提示词管理功能，默认启用
 * promptChangeNotification() - 提示词变更通知，默认启用
 * loggingEnable() - 日志记录功能，默认启用
 * completionEnable() - 自动完成功能，默认启用
 */
@Target({ElementType.TYPE})
@Retention(RetentionPolicy.SOURCE)
@Documented
public @interface McpEndpoint {
    /**
     * 服务名称，唯一标识MCP服务实例
     * 默认值："feat-mcp-server"
     */
    String name() default "feat-mcp-server";

    /**
     * API文档标题，用于生成文档时显示服务标题
     * 默认值："Feat MCP Server"
     */
    String title() default "Feat MCP Server";

    /**
     * 服务版本号，用于标识当前MCP服务的版本
     * 默认值：使用Feat.VERSION常量
     */
    String version() default Feat.VERSION;

    /**
     * MCP SSE（Server-Sent Events）端点路径
     * 必须配置，用于客户端订阅服务器推送事件
     */
    String sseEndpoint();

    /**
     * MCP SSE消息端点路径
     * 必须配置，用于发送特定消息到已订阅的客户端
     */
    String sseMessageEndpoint();

    /**
     * 可流式传输的端点路径
     * 必须配置，用于支持数据流式传输
     */
    String streamableEndpoint();

    /**
     * 是否启用资源管理功能
     * 默认值：true（启用）
     */
    boolean resourceEnable() default true;

    /**
     * 是否启用资源变更通知
     * 默认值：true（启用）
     */
//    boolean resourceChangeNotification() default true;

    /**
     * 是否启用工具管理功能
     * 默认值：true（启用）
     */
    boolean toolEnable() default true;

    /**
     * 是否启用工具变更通知
     * 默认值：true（启用）
     */
//    boolean toolChangeNotification() default true;

    /**
     * 是否启用提示词管理功能
     * 默认值：true（启用）
     */
    boolean promptsEnable() default true;

    /**
     * 是否启用提示词变更通知
     * 默认值：true（启用）
     */
//    boolean promptChangeNotification() default true;

    /**
     * 是否启用日志记录功能
     * 默认值：true（启用）
     */
    boolean loggingEnable() default true;

    /**
     * 是否启用自动完成功能
     * 默认值：true（启用）
     */
//    boolean completionEnable() default true;

}
