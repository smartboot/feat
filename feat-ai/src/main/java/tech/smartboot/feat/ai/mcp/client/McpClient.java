/*
 *  Copyright (C) [2024] smartboot [zhengjunweimail@163.com]
 *
 *  企业用户未经smartboot组织特别许可，需遵循AGPL-3.0开源协议合理合法使用本项目。
 *
 *   Enterprise users are required to use this project reasonably
 *   and legally in accordance with the AGPL-3.0 open source agreement
 *  without special permission from the smartboot organization.
 */

package tech.smartboot.feat.ai.mcp.client;

import com.alibaba.fastjson2.JSONArray;
import com.alibaba.fastjson2.JSONObject;
import tech.smartboot.feat.ai.mcp.enums.LoggerLevel;
import tech.smartboot.feat.ai.mcp.enums.RoleEnum;
import tech.smartboot.feat.ai.mcp.model.McpInitializeRequest;
import tech.smartboot.feat.ai.mcp.model.McpInitializeResponse;
import tech.smartboot.feat.ai.mcp.model.PromptGetResult;
import tech.smartboot.feat.ai.mcp.model.PromptListResponse;
import tech.smartboot.feat.ai.mcp.model.PromptMessage;
import tech.smartboot.feat.ai.mcp.model.Resource;
import tech.smartboot.feat.ai.mcp.model.ResourceListResponse;
import tech.smartboot.feat.ai.mcp.model.Roots;
import tech.smartboot.feat.ai.mcp.model.ToolCalledResult;
import tech.smartboot.feat.ai.mcp.model.ToolListResponse;
import tech.smartboot.feat.ai.mcp.model.ToolResult;
import tech.smartboot.feat.core.client.HttpResponse;
import tech.smartboot.feat.core.common.FeatUtils;
import tech.smartboot.feat.core.common.HttpStatus;
import tech.smartboot.feat.core.common.exception.FeatException;

import java.util.concurrent.CompletableFuture;
import java.util.function.Consumer;

/**
 * MCP (Model Control Protocol) 客户端实现类
 *
 * <p>该类提供了与MCP服务器进行通信的完整客户端功能，支持工具调用、提示词管理、
 * 资源管理等核心MCP协议功能。</p>
 *
 * <p>主要特性：</p>
 * <ul>
 *   <li>支持SSE和Streamable两种传输协议</li>
 *   <li>提供同步和异步API调用方式</li>
 *   <li>完整的工具管理功能</li>
 *   <li>提示词模板管理</li>
 *   <li>资源列表和订阅管理</li>
 *   <li>根目录动态管理</li>
 * </ul>
 *
 * @author 三刀 zhengjunweimail@163.com
 * @version v1.0 7/7/25
 */
public class McpClient {
    private final McpOptions options;
    private final Transport transport;
    private boolean initialized = false;
    private McpInitializeResponse initializeResponse;

    private McpClient(McpOptions options, Transport transport) {
        this.options = options;
        this.transport = transport;
        options.setNotificationHandler(method -> {
            if ("notifications/tools/list_changed".equals(method)) {
                listTools();
            }
        });
    }


    /**
     * 创建基于Streamable协议的MCP客户端（推荐方式）
     *
     * <p>这是创建MCP客户端的标准方法，使用现代的流式传输协议，
     * 提供更好的性能和可靠性。</p>
     *
     * <p>使用示例：</p>
     * <pre>{@code
     * McpClient client = McpClient.newStreamableClient(options -> {
     *     options.setEndpoint("http://localhost:8080/mcp");
     *     options.setTimeout(30000);
     * });
     * }</pre>
     *
     * @param opt MCP配置选项消费者函数，用于配置客户端参数
     * @return 配置好的MCP客户端实例
     */
    public static McpClient streamable(Consumer<McpOptions> opt) {
        McpOptions options = new McpOptions();
        opt.accept(options);
        return new McpClient(options, new StreamableTransport(options));
    }

    /**
     * 异步初始化MCP客户端连接
     *
     * <p>此方法执行MCP协议的初始化握手过程，包括：</p>
     * <ol>
     *   <li>发送initialize请求，包含客户端能力声明</li>
     *   <li>接收服务器响应，获取服务器能力信息</li>
     *   <li>发送initialized通知，确认客户端准备就绪</li>
     * </ol>
     *
     * <p>必须在调用其他MCP方法之前完成初始化。</p>
     *
     * @return 包含初始化结果的CompletableFuture，成功时返回服务器能力信息
     * @throws FeatException 当初始化过程中发生错误时抛出
     * @see #initialize()
     */
    public CompletableFuture<McpInitializeResponse> asyncInitialize() {
        if (initialized) {
            return CompletableFuture.completedFuture(initializeResponse);
        }
        CompletableFuture<McpInitializeResponse> future = new CompletableFuture<>();
        McpInitializeRequest request = new McpInitializeRequest();
        request.setProtocolVersion(McpInitializeRequest.PROTOCOL_VERSION);
        JSONObject capabilitiesJson = new JSONObject();
        if (options.isRoots()) {
            capabilitiesJson.put("roots", JSONObject.of("listChanged", true));
        }
        if (options.isSampling()) {
            capabilitiesJson.put("sampling", new JSONObject());
        }
        if (options.isElicitation()) {
            capabilitiesJson.put("elicitation", new JSONObject());
        }
        if (options.getExperimental() != null) {
            capabilitiesJson.put("experimental", options.getExperimental());
        }
        request.setCapabilities(capabilitiesJson);
        request.setClientInfo(options.getImplementation());

        CompletableFuture<McpInitializeResponse> f = transport.asyncRequest("initialize", JSONObject.from(request), McpInitializeResponse.class);
        f.thenAccept(response -> {
            //After successful initialization, the client MUST send an initialized notification to indicate it is ready to begin normal operations
            CompletableFuture<HttpResponse> notification = transport.sendNotification("notifications/initialized");
            notification.whenComplete((r, e) -> {
                if (e != null) {
                    future.completeExceptionally(e);
                } else if (r.statusCode() == HttpStatus.ACCEPTED.value()) {
                    this.initializeResponse = response;
                    initialized = true;
                    transport.initialized();
                    future.complete(response);
                } else {
                    future.completeExceptionally(new FeatException(r.body()));
                }
            });

        }).exceptionally(throwable -> {
            future.completeExceptionally(throwable);
            return null;
        });
        return future;
    }

    /**
     * 同步初始化MCP客户端连接
     *
     * <p>同步版本的初始化方法，内部调用 {@link #asyncInitialize()} 并等待结果。
     * 如果初始化失败会抛出异常。</p>
     *
     * @return 服务器返回的初始化响应信息
     * @throws FeatException 当初始化过程中发生错误时抛出
     * @see #asyncInitialize()
     */
    public McpInitializeResponse initialize() {
        try {
            return asyncInitialize().get();
        } catch (Throwable e) {
            throw new FeatException(e);
        }
    }

    /**
     * 获取可用工具列表（第一页）
     *
     * <p>获取服务器端注册的所有可用工具的列表。这是一个便捷方法，
     * 相当于调用 {@link #listTools(String)} 并传入null作为游标参数。</p>
     *
     * @return 工具列表响应对象，包含工具信息和分页信息
     * @throws IllegalStateException 当客户端未初始化时抛出
     * @throws FeatException         当请求过程中发生错误时抛出
     * @see #listTools(String)
     * @see #asyncListTools(String)
     */
    public ToolListResponse listTools() {
        return listTools(null);
    }

    /**
     * 异步获取工具列表
     *
     * <p>异步方式获取可用工具列表，支持分页查询。如果需要获取后续页面，
     * 可以使用前一次响应中的cursor值作为参数。</p>
     *
     * @param nextCursor 分页游标，null表示获取第一页
     * @return 包含工具列表的CompletableFuture
     * @throws IllegalStateException 当客户端未初始化时抛出
     * @see #listTools(String)
     * @see ToolListResponse
     */
    public CompletableFuture<ToolListResponse> asyncListTools(String nextCursor) {
        stateCheck();
        JSONObject param = new JSONObject();
        if (FeatUtils.isNotBlank(nextCursor)) {
            param.put("cursor", nextCursor);
        }
        return transport.asyncRequest("tools/list", param, ToolListResponse.class);
    }

    /**
     * 同步获取工具列表
     *
     * <p>同步方式获取可用工具列表，支持分页查询。如果需要获取后续页面，
     * 可以使用前一次响应中的cursor值作为参数。</p>
     *
     * @param nextCursor 分页游标，null表示获取第一页
     * @return 工具列表响应对象
     * @throws IllegalStateException 当客户端未初始化时抛出
     * @throws FeatException         当请求过程中发生错误时抛出
     * @see #asyncListTools(String)
     */
    public ToolListResponse listTools(String nextCursor) {
        try {
            return asyncListTools(nextCursor).get();
        } catch (Throwable e) {
            throw new FeatException(e);
        }
    }

    /**
     * 获取可用提示词列表（第一页）
     *
     * <p>获取服务器端注册的所有可用提示词模板的列表。这是一个便捷方法，
     * 相当于调用 {@link #listPrompts(String)} 并传入null作为游标参数。</p>
     *
     * @return 提示词列表响应对象
     * @throws IllegalStateException 当客户端未初始化时抛出
     * @throws FeatException         当请求过程中发生错误时抛出
     * @see #listPrompts(String)
     * @see #asyncListPrompts(String)
     */
    public PromptListResponse listPrompts() {
        return listPrompts(null);
    }


    /**
     * 异步调用指定工具
     *
     * <p>异步方式调用服务器端注册的工具，并传递必要的参数。
     * 工具执行完成后返回结果，结果可能包含多种类型的内容。</p>
     *
     * <p>使用示例：</p>
     * <pre>{@code
     * JSONObject args = new JSONObject();
     * args.put("filePath", "/path/to/file");
     * args.put("operation", "read");
     *
     * CompletableFuture<ToolCalledResult> future = client.asyncCallTool("file_operations", args);
     * ToolCalledResult result = future.get();
     *
     * if (!result.isError()) {
     *     for (ToolResult content : result.getContent()) {
     *         System.out.println(content);
     *     }
     * }
     * }</pre>
     *
     * @param toolName  要调用的工具名称
     * @param arguments 工具参数，可以为null
     * @return 包含工具执行结果的CompletableFuture
     * @throws IllegalStateException 当客户端未初始化时抛出
     * @see #callTool(String, JSONObject)
     * @see ToolCalledResult
     * @see ToolResult
     */
    public CompletableFuture<ToolCalledResult> asyncCallTool(String toolName, JSONObject arguments) {
        stateCheck();
        JSONObject param = new JSONObject();
        param.put("name", toolName);
        if (arguments != null) {
            param.put("arguments", arguments);
        }
        CompletableFuture<ToolCalledResult> future = new CompletableFuture<>();
        CompletableFuture<JSONObject> f = transport.asyncRequest("tools/call", param, JSONObject.class);
        f.thenAccept(response -> {
            JSONArray content = response.getJSONArray("content");
            ToolCalledResult callToolResult = new ToolCalledResult();
            callToolResult.setError(response.getBooleanValue("isError"));
            for (int i = 0; i < content.size(); i++) {
                JSONObject contentItem = content.getJSONObject(i);
                String type = contentItem.getString("type");
                switch (type) {
                    case "text":
                        callToolResult.addContent(contentItem.to(ToolResult.TextContent.class));
                        break;
                    case "image":
                        callToolResult.addContent(contentItem.to(ToolResult.ImageContent.class));
                        break;
                    case "audio":
                        callToolResult.addContent(contentItem.to(ToolResult.AudioContent.class));
                        break;
                    case "resource_link":
                        callToolResult.addContent(contentItem.to(ToolResult.ResourceLinks.class));
                        break;
                    default:
                        callToolResult.addContent(ToolResult.ofStructuredContent(content.getJSONObject(i)));
                }
            }
            future.complete(callToolResult);
        }).exceptionally(throwable -> {
            future.completeExceptionally(throwable);
            return null;
        });
        return future;
    }

    /**
     * 调用指定工具（无参数版本）
     *
     * <p>便捷方法，用于调用不需要参数的工具。
     * 相当于调用 {@link #callTool(String, JSONObject)} 并传入null作为参数。</p>
     *
     * @param toolName 要调用的工具名称
     * @return 工具执行结果
     * @throws IllegalStateException 当客户端未初始化时抛出
     * @throws FeatException         当请求过程中发生错误时抛出
     * @see #callTool(String, JSONObject)
     * @see #asyncCallTool(String, JSONObject)
     */
    public ToolCalledResult callTool(String toolName) {
        return callTool(toolName, null);
    }


    /**
     * 同步调用指定工具
     *
     * <p>同步方式调用服务器端注册的工具，并传递必要的参数。
     * 这是 {@link #asyncCallTool(String, JSONObject)} 的同步包装版本。</p>
     *
     * @param toolName  要调用的工具名称
     * @param arguments 工具参数，可以为null
     * @return 工具执行结果
     * @throws IllegalStateException 当客户端未初始化时抛出
     * @throws FeatException         当请求过程中发生错误时抛出
     * @see #asyncCallTool(String, JSONObject)
     */
    public ToolCalledResult callTool(String toolName, JSONObject arguments) {
        try {
            return asyncCallTool(toolName, arguments).get();
        } catch (Throwable e) {
            throw new FeatException(e);
        }
    }

    /**
     * 异步获取提示词列表
     *
     * <p>异步方式获取可用提示词模板列表，支持分页查询。
     * 提示词模板可用于生成标准化的对话内容。</p>
     *
     * @param nextCursor 分页游标，null表示获取第一页
     * @return 包含提示词列表的CompletableFuture
     * @throws IllegalStateException 当客户端未初始化时抛出
     * @see #listPrompts(String)
     * @see PromptListResponse
     */
    public CompletableFuture<PromptListResponse> asyncListPrompts(String nextCursor) {
        stateCheck();
        JSONObject param = new JSONObject();
        if (FeatUtils.isNotBlank(nextCursor)) {
            param.put("cursor", nextCursor);
        }
        return transport.asyncRequest("prompts/list", param, PromptListResponse.class);
    }

    /**
     * 同步获取提示词列表
     *
     * <p>同步方式获取可用提示词模板列表，支持分页查询。
     * 这是 {@link #asyncListPrompts(String)} 的同步包装版本。</p>
     *
     * @param nextCursor 分页游标，null表示获取第一页
     * @return 提示词列表响应对象
     * @throws IllegalStateException 当客户端未初始化时抛出
     * @throws FeatException         当请求过程中发生错误时抛出
     * @see #asyncListPrompts(String)
     */
    public PromptListResponse listPrompts(String nextCursor) {
        try {
            return asyncListPrompts(nextCursor).get();
        } catch (Throwable e) {
            throw new FeatException(e);
        }
    }

    /**
     * 异步获取指定提示词模板
     *
     * <p>根据提示词名称获取具体的提示词模板内容，可选择性地传递参数
     * 来定制化提示词内容。</p>
     *
     * <p>使用示例：</p>
     * <pre>{@code
     * JSONObject params = new JSONObject();
     * params.put("topic", "Java编程");
     * params.put("difficulty", "中级");
     *
     * CompletableFuture<PromptGetResult> future = client.asyncGetPrompt("coding_assistant", params);
     * PromptGetResult result = future.get();
     *
     * for (PromptMessage<?> message : result.getMessages()) {
     *     System.out.println(message.getRole() + ": " + message.getContent());
     * }
     * }</pre>
     *
     * @param name      提示词模板名称
     * @param arguments 模板参数，可以为null
     * @return 包含提示词内容的CompletableFuture
     * @throws IllegalStateException 当客户端未初始化时抛出
     * @see #getPrompt(String, JSONObject)
     * @see PromptGetResult
     * @see PromptMessage
     */
    public CompletableFuture<PromptGetResult> asyncGetPrompt(String name, JSONObject arguments) {
        stateCheck();
        JSONObject param = new JSONObject();
        param.put("name", name);
        if (arguments != null) {
            param.put("arguments", arguments);
        }
        CompletableFuture<JSONObject> f = transport.asyncRequest("prompts/get", param, JSONObject.class);
        CompletableFuture<PromptGetResult> future = new CompletableFuture<>();
        f.thenAccept(response -> {
            JSONObject result = response;
            PromptGetResult promptMessage = new PromptGetResult();
            promptMessage.setDescription(result.getString("description"));
            JSONArray messages = result.getJSONArray("messages");
            for (int i = 0; i < messages.size(); i++) {
                JSONObject message = messages.getJSONObject(i);
                JSONObject content = message.getJSONObject("content");
                String type = content.getString("type");
                RoleEnum roleEnum = RoleEnum.of(message.getString("role"));
                PromptMessage<?> promptResult = null;
                switch (type) {
                    case "text":
                        promptResult = PromptMessage.ofText(roleEnum, content.getString("text"));
                        break;
                    case "image":
                        promptResult = PromptMessage.ofImage(roleEnum, content.getString("data"), content.getString("mimeType"));
                        break;
                    case "audio":
                        promptResult = PromptMessage.ofAudio(roleEnum, content.getString("data"), content.getString("mimeType"));
                        break;
                    case "resource":
                        JSONObject resource = content.getJSONObject("resource");
                        promptResult = PromptMessage.ofEmbeddedResource(roleEnum, resource.to(Resource.class));
                        break;
                    default:
                        throw new FeatException("unknown prompt content type: " + type);
                }
                promptMessage.getMessages().add(promptResult);
                future.complete(promptMessage);
            }
        }).exceptionally(throwable -> {
            future.completeExceptionally(throwable);
            return null;
        });
        return future;
    }

    /**
     * 获取指定提示词模板（无参数版本）
     *
     * <p>便捷方法，用于获取不需要参数的提示词模板。
     * 相当于调用 {@link #getPrompt(String, JSONObject)} 并传入null作为参数。</p>
     *
     * @param name 提示词模板名称
     * @return 提示词获取结果
     * @throws IllegalStateException 当客户端未初始化时抛出
     * @throws FeatException         当请求过程中发生错误时抛出
     * @see #getPrompt(String, JSONObject)
     * @see #asyncGetPrompt(String, JSONObject)
     */
    public PromptGetResult getPrompt(String name) {
        return getPrompt(name, null);
    }

    /**
     * 同步获取指定提示词模板
     *
     * <p>同步方式获取提示词模板内容，这是 {@link #asyncGetPrompt(String, JSONObject)}
     * 的同步包装版本。</p>
     *
     * @param name      提示词模板名称
     * @param arguments 模板参数，可以为null
     * @return 提示词获取结果
     * @throws IllegalStateException 当客户端未初始化时抛出
     * @throws FeatException         当请求过程中发生错误时抛出
     * @see #asyncGetPrompt(String, JSONObject)
     */
    public PromptGetResult getPrompt(String name, JSONObject arguments) {
        try {
            return asyncGetPrompt(name, arguments).get();
        } catch (Throwable e) {
            throw new FeatException(e);
        }
    }

    /**
     * 设置客户端日志级别
     *
     * <p>配置MCP客户端的日志输出级别，控制调试信息的详细程度。
     * <strong>注意：</strong>当前实现为空，待完善。</p>
     *
     * @param level 日志级别枚举值
     * @see LoggerLevel
     */
    public void setLoggingLevel(LoggerLevel level) {
//transport.sendNotification()
    }

    /**
     * 获取可用资源列表（第一页）
     *
     * <p>获取服务器端可访问的资源列表。这是一个便捷方法，
     * 相当于调用 {@link #listResources(String)} 并传入null作为游标参数。</p>
     *
     * @return 资源列表响应对象
     * @throws IllegalStateException 当客户端未初始化时抛出
     * @throws FeatException         当请求过程中发生错误时抛出
     * @see #listResources(String)
     * @see #asyncListResources(String)
     */
    public ResourceListResponse listResources() {
        return listResources(null);
    }

    /**
     * 异步获取资源列表
     *
     * <p>异步方式获取可用资源列表，支持分页查询。
     * 资源可以是文件、数据库连接等各种可访问的数据源。</p>
     *
     * @param nextCursor 分页游标，null表示获取第一页
     * @return 包含资源列表的CompletableFuture
     * @throws IllegalStateException 当客户端未初始化时抛出
     * @see #listResources(String)
     * @see ResourceListResponse
     */
    public CompletableFuture<ResourceListResponse> asyncListResources(String nextCursor) {
        stateCheck();
        JSONObject param = new JSONObject();
        if (FeatUtils.isNotBlank(nextCursor)) {
            param.put("cursor", nextCursor);
        }
        return transport.asyncRequest("resources/list", param, ResourceListResponse.class);
    }

    /**
     * 同步获取资源列表
     *
     * <p>同步方式获取可用资源列表，支持分页查询。
     * 这是 {@link #asyncListResources(String)} 的同步包装版本。</p>
     *
     * @param nextCursor 分页游标，null表示获取第一页
     * @return 资源列表响应对象
     * @throws IllegalStateException 当客户端未初始化时抛出
     * @throws FeatException         当请求过程中发生错误时抛出
     * @see #asyncListResources(String)
     */
    public ResourceListResponse listResources(String nextCursor) {
        try {
            return asyncListResources(nextCursor).get();
        } catch (Throwable e) {
            throw new FeatException(e);
        }
    }

    /**
     * 添加根目录
     *
     * <p>动态添加新的根目录到客户端配置中。根目录用于限定工具和资源的访问范围。
     * 添加后会自动发送根目录变更通知给服务器。</p>
     *
     * <p><strong>前提条件：</strong>客户端必须启用roots功能</p>
     *
     * @param uri  根目录URI
     * @param name 根目录显示名称
     * @throws FeatException         当roots功能未启用时抛出
     * @throws IllegalStateException 当客户端未初始化时抛出
     */
    public void addRoot(String uri, String name) {
        if (!options.isRoots()) {
            throw new FeatException("roots is not enabled");
        }
        options.getRootsList().add(new Roots(uri, name));
        transport.sendNotification("notifications/roots/list_changed");
    }

    /**
     * 移除根目录
     *
     * <p>从客户端配置中移除指定的根目录。移除后会自动发送根目录变更通知给服务器。</p>
     *
     * <p><strong>前提条件：</strong>客户端必须启用roots功能且已完成初始化</p>
     *
     * @param uri 要移除的根目录URI
     * @throws FeatException         当roots功能未启用时抛出
     * @throws IllegalStateException 当客户端未初始化时抛出
     * @see #addRoot(String, String)
     */
    public void removeRoot(String uri) {
        stateCheck();
        if (!options.isRoots()) {
            throw new FeatException("roots is not enabled");
        }
        options.getRootsList().removeIf(roots -> roots.getUri().equals(uri));
        transport.sendNotification("notifications/roots/list_changed");
    }
//    public void subscribeResource(String uri) {
//
//        CompletableFuture<Response<JSONObject>> f = transport.asyncRequest("resources/subscribe", JSONObject.of("uri",uri));
//    }

    /**
     * 检查客户端状态
     *
     * <p>验证客户端是否已完成初始化，大多数MCP操作都需要在初始化完成后才能执行。
     * 如果客户端未初始化则抛出IllegalStateException异常。</p>
     *
     * @throws IllegalStateException 当客户端未初始化时抛出
     */
    private void stateCheck() {
        if (!initialized) {
            throw new IllegalStateException("McpClient not initialized");
        }
    }

    /**
     * 关闭客户端连接
     *
     * <p>释放客户端占用的资源，关闭与服务器的连接。
     * 调用此方法后客户端将无法继续使用，需要重新创建新的实例。</p>
     *
     * <p><strong>建议：</strong>在应用程序结束前调用此方法确保资源正确释放。</p>
     *
     * @see Transport#close()
     */
    public void close() {
        transport.close();
    }
}
