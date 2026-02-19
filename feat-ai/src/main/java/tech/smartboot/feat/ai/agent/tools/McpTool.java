/*
 *  Copyright (C) [2024] smartboot [zhengjunweimail@163.com]
 *
 *  企业用户未经smartboot组织特别许可，需遵循AGPL-3.0开源协议合理合法使用本项目。
 *
 *   Enterprise users are required to use this project reasonably
 *   and legally in accordance with the AGPL-3.0 open source agreement
 *  without special permission from the smartboot organization.
 */

package tech.smartboot.feat.ai.agent.tools;

import com.alibaba.fastjson2.JSONObject;
import tech.smartboot.feat.ai.agent.AgentOptions;
import tech.smartboot.feat.ai.agent.AgentTool;
import tech.smartboot.feat.ai.mcp.client.McpClient;
import tech.smartboot.feat.ai.mcp.model.McpInitializeResponse;
import tech.smartboot.feat.ai.mcp.model.Tool;
import tech.smartboot.feat.ai.mcp.model.ToolCalledResult;
import tech.smartboot.feat.ai.mcp.model.ToolResult;
import tech.smartboot.feat.core.common.FeatUtils;

import java.util.concurrent.CompletableFuture;

/**
 * MCP (Model Control Protocol) 工具适配器类
 *
 * <p>该类实现了AgentTool接口，将MCP协议中的工具封装为Agent可直接使用的工具格式。
 * 主要负责工具的注册、执行和结果格式化等功能。</p>
 *
 * <p>主要特性：</p>
 * <ul>
 *   <li>自动注册MCP服务器提供的所有工具</li>
 *   <li>支持工具名称的自定义命名规则</li>
 *   <li>统一的结果格式化处理</li>
 *   <li>支持多种内容类型的处理（文本、图片、音频等）</li>
 * </ul>
 *
 * @author 三刀 zhengjunweimail@163.com
 * @version v1.0.0
 */
public class McpTool implements AgentTool {

    private final McpClient mcpClient;
    private final Tool tool;

    /**
     * 构造函数，创建MCP工具适配器实例
     *
     * <p>将MCP客户端和具体工具定义包装为AgentTool接口实现，
     * 便于Agent框架统一管理和调用。</p>
     *
     * @param client MCP客户端实例，用于与MCP服务器通信
     * @param tool   MCP工具定义对象，包含工具的元数据信息
     */
    public McpTool(McpClient client, Tool tool) {
        this.mcpClient = client;
        this.tool = tool;
    }


    /**
     * 注册MCP工具到Agent选项中
     *
     * <p>此方法会初始化MCP客户端连接，获取服务器提供的所有工具列表，
     * 并将每个工具注册为Agent可使用的工具。工具名称会根据服务器信息
     * 进行智能命名，格式为：服务器名_工具名。</p>
     *
     * <p>使用示例：</p>
     * <pre>{@code
     * // 创建 MCP 客户端
     * McpClient mcpClient = McpClient.streamable(opt -> {
     *     opt.debug(false).url("https://remote.mcpservers.org/fetch/mcp");
     * });
     * mcpClient.initialize();
     * 
     * // 创建 ReActAgent 并注册 MCP 工具
     * FeatAgent agent = new ReActAgent(opt -> {
     *     McpTool.register(opt, mcpClient);
     *     // 配置其他选项...
     * });
     * 
     * // 使用 Agent 执行任务，它会智能决策何时调用 MCP 工具
     * String result = agent.execute("查询smart-socket的最新版本").get();
     * }</pre>
     *
     * @param options    Agent配置选项，用于注册工具
     * @param mcpClient  MCP客户端实例
     * @return 初始化并注册了所有工具的MCP客户端
     * @see AgentOptions#tool(AgentTool)
     * @see McpClient#initialize()
     * @see McpClient#listTools()
     * @see McpClient#streamable(java.util.function.Consumer)
     */
    public static McpClient register(AgentOptions options, McpClient mcpClient) {
        McpInitializeResponse initialize = mcpClient.initialize();
        mcpClient.listTools().getTools().forEach(tool -> {
            options.tool(new McpTool(mcpClient, tool) {
                @Override
                public String getName() {
                    if (FeatUtils.isNotBlank(initialize.getServerInfo().getName())) {
                        return initialize.getServerInfo().getName() + "_" + tool.getName();
                    } else {
                        return super.getName();
                    }
                }
            });
        });
        return mcpClient;
    }

    /**
     * 执行MCP工具调用
     *
     * <p>异步执行MCP工具调用，将参数传递给底层MCP客户端进行远程调用，
     * 并对返回的结果进行格式化处理。此方法内部调用
     * {@link McpClient#asyncCallTool(String, com.alibaba.fastjson2.JSONObject)} 方法。</p>
     *
     * <p>执行流程：</p>
     * <ol>
     *   <li>通过MCP客户端异步调用指定工具</li>
     *   <li>接收工具执行结果</li>
     *   <li>调用formatResult方法格式化结果</li>
     *   <li>返回格式化后的字符串结果</li>
     * </ol>
     *
     * @param parameters 工具调用参数，JSON格式
     * @return 包含工具执行结果的CompletableFuture
     * @see McpClient#asyncCallTool(String, com.alibaba.fastjson2.JSONObject)
     * @see #formatResult(ToolCalledResult)
     */
    @Override
    public CompletableFuture<String> execute(JSONObject parameters) {
        return mcpClient.asyncCallTool(tool.getName(), parameters).thenApply(this::formatResult);
    }

    /**
     * 格式化工具执行结果为字符串
     *
     * <p>将MCP工具调用返回的复杂结果对象转换为适合Agent使用的字符串格式。
     * 对于错误结果会在前面加上"Error: "前缀。此方法处理来自
     * {@link McpClient#asyncCallTool(String, com.alibaba.fastjson2.JSONObject)} 的结果。</p>
     *
     * <p>处理逻辑：</p>
     * <ul>
     *   <li>空结果返回空字符串</li>
     *   <li>错误结果添加"Error: "前缀</li>
     *   <li>正常结果调用extractContent提取具体内容</li>
     * </ul>
     *
     * @param result MCP工具调用结果对象
     * @return 格式化后的字符串结果，可能包含错误信息
     * @see #extractContent(ToolCalledResult)
     * @see tech.smartboot.feat.ai.mcp.model.ToolCalledResult#isError()
     */
    private String formatResult(ToolCalledResult result) {
        if (result == null) {
            return "";
        }
        if (result.isError()) {
            return "Error: " + extractContent(result);
        }
        return extractContent(result);
    }

    /**
     * 从工具调用结果中提取具体内容
     *
     * <p>遍历结果中的所有内容项，根据不同类型进行相应的处理和转换。
     * 处理来自 {@link McpClient#asyncCallTool(String, com.alibaba.fastjson2.JSONObject)} 的结果：</p>
     * <ul>
     *   <li><strong>文本内容({@link ToolResult.TextContent})：</strong>直接提取文本内容</li>
     *   <li><strong>图片内容({@link ToolResult.ImageContent})：</strong>返回"[Image]"占位符</li>
     *   <li><strong>音频内容({@link ToolResult.AudioContent})：</strong>返回"[Audio]"占位符</li>
     *   <li><strong>资源链接({@link ToolResult.ResourceLinks})：</strong>返回"[Resource]"占位符</li>
     *   <li><strong>结构化内容({@link ToolResult.StructuredContent})：</strong>转换为JSON字符串</li>
     * </ul>
     *
     * @param result 工具调用结果对象，包含多种类型的内容
     * @return 合并后的字符串内容
     * @see ToolResult.TextContent
     * @see ToolResult.ImageContent
     * @see ToolResult.AudioContent
     * @see ToolResult.ResourceLinks
     * @see ToolResult.StructuredContent
     * @see McpClient#asyncCallTool(String, com.alibaba.fastjson2.JSONObject)
     */
    private String extractContent(ToolCalledResult result) {
        if (result.getContent() == null || result.getContent().isEmpty()) {
            return "";
        }
        StringBuilder sb = new StringBuilder();
        for (ToolResult content : result.getContent()) {
            if (content instanceof ToolResult.TextContent) {
                sb.append(((ToolResult.TextContent) content).getText());
            } else if (content instanceof ToolResult.ImageContent) {
                sb.append("[Image]");
            } else if (content instanceof ToolResult.AudioContent) {
                sb.append("[Audio]");
            } else if (content instanceof ToolResult.ResourceLinks) {
                sb.append("[Resource]");
            } else if (content instanceof ToolResult.StructuredContent) {
                sb.append(((ToolResult.StructuredContent) content).getContent().toJSONString());
            }
        }
        return sb.toString();
    }

    /**
     * 获取工具名称
     *
     * <p>返回工具的唯一标识名称。默认实现使用哈希码和原始工具名称的组合
     * 来确保名称的唯一性。在 {@link #register(AgentOptions, McpClient)} 方法中，
     * 会通过匿名子类重写此方法，使用服务器名+工具名的格式。</p>
     *
     * <p>默认命名规则：mcpTool + 哈希码 + _ + 原始工具名称</p>
     * <p>重写后命名规则：服务器名_工具名</p>
     *
     * @return 工具的唯一名称标识
     * @see #register(AgentOptions, McpClient)
     * @see tech.smartboot.feat.ai.mcp.model.Tool#getName()
     */
    @Override
    public String getName() {
        return "mcpTool" + this.hashCode() + "_" + tool.getName();
    }

    /**
     * 获取工具描述信息
     *
     * <p>返回工具的详细描述，包括基本描述和注解信息（如果存在）。
     * 描述信息对于Agent理解和选择合适的工具非常重要。
     * 此方法从底层 {@link tech.smartboot.feat.ai.mcp.model.Tool} 对象获取描述信息。</p>
     *
     * <p>格式：基础描述 + (annotations:注解信息)</p>
     *
     * @return 工具的完整描述信息
     * @see tech.smartboot.feat.ai.mcp.model.Tool#getDescription()
     * @see tech.smartboot.feat.ai.mcp.model.Tool#getAnnotations()
     */
    @Override
    public String getDescription() {
        return tool.getDescription() + (tool.getAnnotations() == null ? "" : (" annotations:" + tool.getAnnotations()));
    }

    /**
     * 获取工具参数模式定义
     *
     * <p>返回工具输入参数的JSON Schema定义，用于参数验证和自动补全。
     * Schema定义了工具期望接收的参数结构、类型和约束条件。
     * 此方法从底层 {@link tech.smartboot.feat.ai.mcp.model.Tool} 对象获取Schema信息。</p>
     *
     * @return 参数模式的JSON字符串表示
     * @see tech.smartboot.feat.ai.mcp.model.Tool#getInputSchema()
     * @see com.alibaba.fastjson2.JSONObject#from(Object)
     */
    @Override
    public String getParametersSchema() {
        return JSONObject.from(tool.getInputSchema()).toJSONString();
    }

}
