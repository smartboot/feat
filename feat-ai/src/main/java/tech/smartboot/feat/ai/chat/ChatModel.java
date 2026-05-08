/*
 *  Copyright (C) [2024] smartboot [zhengjunweimail@163.com]
 *
 *  企业用户未经smartboot组织特别许可，需遵循Apache-2.0开源协议合理合法使用本项目。
 *
 *   Enterprise users are required to use this project reasonably
 *   and legally in accordance with the Apache-2.0 open source agreement
 *  without special permission from the smartboot organization.
 */

package tech.smartboot.feat.ai.chat;

import com.alibaba.fastjson2.JSON;
import com.alibaba.fastjson2.JSONArray;
import com.alibaba.fastjson2.JSONObject;
import com.alibaba.fastjson2.TypeReference;
import tech.smartboot.feat.Feat;
import tech.smartboot.feat.ai.chat.entity.ChatWholeResponse;
import tech.smartboot.feat.ai.chat.entity.Message;
import tech.smartboot.feat.ai.chat.entity.ResponseMessage;
import tech.smartboot.feat.ai.chat.entity.StreamResponseCallback;
import tech.smartboot.feat.ai.chat.entity.Tool;
import tech.smartboot.feat.ai.chat.entity.ToolCall;
import tech.smartboot.feat.ai.chat.entity.Usage;
import tech.smartboot.feat.core.client.HttpPost;
import tech.smartboot.feat.core.common.FeatUtils;
import tech.smartboot.feat.core.common.HeaderName;
import tech.smartboot.feat.core.common.exception.FeatException;
import tech.smartboot.feat.core.common.logging.Logger;
import tech.smartboot.feat.core.common.logging.LoggerFactory;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.Consumer;

/**
 * 聊天模型类，用于与AI模型进行交互，支持流式和非流式响应
 * 支持 OpenAI 和 Anthropic 两种API规范
 *
 * @author 三刀 zhengjunweimail@163.com
 * @version v1.0.0
 */
public class ChatModel {
    private static final Logger LOGGER = LoggerFactory.getLogger(ChatModel.class);
    private final ChatOptions options;
    private static final int STREAM_STATUS_INIT = 0;
    private static final int STREAM_STATUS_UPGRADE = 1;
    private static final int STREAM_STATUS_COMPLETE = 2;
    private static final int STREAM_STATUS_ERROR = 3;

    /**
     * Anthropic API 版本头
     */
    private static final String ANTHROPIC_VERSION = "2023-06-01";

    /**
     * 构造函数
     *
     * @param options 聊天选项配置
     */
    public ChatModel(ChatOptions options) {
        if (options.baseUrl().endsWith("/")) {
            options.baseUrl(options.baseUrl().substring(0, options.baseUrl().length() - 1));
        }
        this.options = options;
    }

    /**
     * 发送流式聊天请求（简单版本）
     *
     * @param content  用户输入内容
     * @param consumer 流式响应回调
     */
    public void chatStream(String content, StreamResponseCallback consumer) {
        chatStream(Collections.singletonList(Message.ofUser(content)), consumer);
    }

    /**
     * 发送流式聊天请求（完整版本）
     *
     * @param messages 消息列表
     * @param consumer 流式响应回调
     */
    public void chatStream(List<Message> messages, StreamResponseCallback consumer) {
        HttpPost post;
        Map<Integer, ToolCall> toolCallMap = new HashMap<>();
        AtomicInteger status = new AtomicInteger(STREAM_STATUS_INIT);
        if (options.getApiSpec() == ApiSpec.ANTHROPIC) {
            post = buildAnthropicRequest(messages, true);
            anthropicStream(post, consumer, status);
        } else {
            post = buildOpenAIRequest(messages, true);
            openAIStream(post, consumer, toolCallMap, status);
        }
        post.onSuccess(response -> {
            if (status.get() == STREAM_STATUS_INIT) {
                status.set(STREAM_STATUS_ERROR);
                consumer.onError(new FeatException(response.body()));
            }
        }).onFailure(throwable -> {
            status.set(STREAM_STATUS_ERROR);
            consumer.onError(throwable);
        }).submit();
    }

    /**
     * 处理 OpenAI 格式的流式响应
     */
    private void openAIStream(HttpPost post, StreamResponseCallback consumer, Map<Integer, ToolCall> toolCallMap, AtomicInteger status) {
        StringBuilder contentBuilder = new StringBuilder();
        StringBuilder reasoningBuilder = new StringBuilder();
        post.onSSE(sse -> sse.onData(event -> {
            if (status.get() == STREAM_STATUS_INIT) {
                status.set(STREAM_STATUS_UPGRADE);
            }
            String data = event.getData();
            if ("[DONE]".equals(data) || FeatUtils.isBlank(data)) {
                if (status.get() == STREAM_STATUS_COMPLETE && contentBuilder.length() == 0) {
                    return;
                }
                ResponseMessage responseMessage = new ResponseMessage();
                responseMessage.setRole(Message.ROLE_ASSISTANT);
                responseMessage.setContent(contentBuilder.toString());
                responseMessage.setReasoningContent(reasoningBuilder.toString());
                responseMessage.setToolCalls(new ArrayList<>(toolCallMap.values()));
                responseMessage.setSuccess(true);
                status.set(STREAM_STATUS_COMPLETE);
                consumer.onCompletion(responseMessage);
                return;
            }
            JSONObject object = JSON.parseObject(data);
            JSONObject error = object.getJSONObject("error");
            if (error != null) {
                ResponseMessage responseMessage = new ResponseMessage();
                responseMessage.setRole(Message.ROLE_ASSISTANT);
                responseMessage.setError(error.getString("message"));
                responseMessage.setSuccess(false);
                status.set(STREAM_STATUS_COMPLETE);
                consumer.onCompletion(responseMessage);
                return;
            }
            //最后一个可能为空
            JSONArray choices = object.getJSONArray("choices");
            if (choices == null || choices.isEmpty()) {
                return;
            }
            JSONObject choice = choices.getJSONObject(0);
            JSONObject delta = choice.getJSONObject("delta");
            if (delta == null) {
                LOGGER.error("delta is null");
                return;
            }
            String content = delta.getString("content");
            if (content != null) {
                consumer.onStreamResponse(content);
                contentBuilder.append(content);
            }
            String reasoningContent = delta.getString("reasoning_content");
            if (reasoningContent != null) {
                consumer.onReasoning(reasoningContent);
                reasoningBuilder.append(reasoningContent);
            }
            List<ToolCall> toolCalls = delta.getObject("tool_calls", new TypeReference<List<ToolCall>>() {
            });
            if (FeatUtils.isNotEmpty(toolCalls)) {
                for (ToolCall toolCall : toolCalls) {
                    ToolCall tool = toolCallMap.computeIfAbsent(toolCall.getIndex(), k -> {
                        ToolCall t = new ToolCall();
                        t.setFunction(new HashMap<>());
                        return t;
                    });
                    if (FeatUtils.isNotBlank(toolCall.getId())) {
                        tool.setId(toolCall.getId());
                    }
                    if (FeatUtils.isNotBlank(toolCall.getType())) {
                        tool.setType(toolCall.getType());
                    }
                    if (toolCall.getFunction() != null) {
                        toolCall.getFunction().forEach((k, v) -> {
                            if (v == null) {
                                return;
                            }
                            String preV = tool.getFunction().get(k);
                            if (FeatUtils.isNotBlank(preV)) {
                                tool.getFunction().put(k, preV + v);
                            } else {
                                tool.getFunction().put(k, v);
                            }
                        });
                    }
                }
            }
        }));
    }

    /**
     * 处理 Anthropic 格式的流式响应
     */
    private void anthropicStream(HttpPost post, StreamResponseCallback consumer, AtomicInteger status) {
        StringBuilder contentBuilder = new StringBuilder();
        StringBuilder reasoningBuilder = new StringBuilder();
        post.onSSE(sse -> sse.onData(event -> {
            if (status.get() == STREAM_STATUS_INIT) {
                status.set(STREAM_STATUS_UPGRADE);
            }

            String eventType = event.getType();
            String data = event.getData();

            if (FeatUtils.isBlank(data)) {
                return;
            }

            try {
                JSONObject object = JSON.parseObject(data);

                switch (eventType) {
                    case "message_start":
                        // 消息开始，无需处理
                        break;
                    case "content_block_start":
                        // 内容块开始
                        break;
                    case "content_block_delta":
                        JSONObject delta = object.getJSONObject("delta");
                        if (delta != null) {
                            String text = delta.getString("text");
                            if (text != null) {
                                consumer.onStreamResponse(text);
                                contentBuilder.append(text);
                            }
                            String thinking = delta.getString("thinking");
                            if (thinking != null) {
                                consumer.onReasoning(thinking);
                                reasoningBuilder.append(thinking);
                            }
                        }
                        break;
                    case "content_block_stop":
                        // 内容块结束
                        break;
                    case "message_delta":
                        // 消息更新（如停止原因）
                        break;
                    case "message_stop":
                        // 消息结束，构建完整响应
                        ResponseMessage responseMessage = new ResponseMessage();
                        responseMessage.setRole(Message.ROLE_ASSISTANT);
                        responseMessage.setContent(contentBuilder.toString());
                        responseMessage.setReasoningContent(reasoningBuilder.toString());
                        responseMessage.setSuccess(true);
                        status.set(STREAM_STATUS_COMPLETE);
                        consumer.onCompletion(responseMessage);
                        break;
                    default:
                        LOGGER.debug("Unknown Anthropic event type: " + eventType);
                }
            } catch (Exception e) {
                LOGGER.error("Error parsing Anthropic stream response", e);
            }
        }));
    }

    /**
     * 发送非流式聊天请求（消息列表版本）
     *
     * @param messages 消息列表
     * @return 包含响应消息的CompletableFuture
     */
    public CompletableFuture<ResponseMessage> chat(List<Message> messages) {
        CompletableFuture<ResponseMessage> future = new CompletableFuture<>();
        chat(messages, future::complete);
        return future;
    }

    /**
     * 发送非流式聊天请求（工具版本）
     *
     * @param content 用户输入内容
     * @return 包含响应消息的CompletableFuture
     */
    public CompletableFuture<ResponseMessage> chat(String content) {
        CompletableFuture<ResponseMessage> future = new CompletableFuture<>();
        chat(Collections.singletonList(Message.ofUser(content)), future::complete);
        return future;
    }

    /**
     * 发送非流式聊天请求（回调版本）
     *
     * @param messages 消息列表
     * @param callback 响应回调
     */
    public void chat(List<Message> messages, Consumer<ResponseMessage> callback) {
        HttpPost post;
        if (options.getApiSpec() == ApiSpec.ANTHROPIC) {
            post = buildAnthropicRequest(messages, false);
            post.onSuccess(response -> {
                if (response.statusCode() != 200) {
                    ResponseMessage responseMessage = new ResponseMessage();
                    responseMessage.setRole(Message.ROLE_ASSISTANT);
                    responseMessage.setError(response.body());
                    responseMessage.setSuccess(false);
                    callback.accept(responseMessage);
                    return;
                }
                JSONObject object = JSON.parseObject(response.body());
                ResponseMessage responseMessage = new ResponseMessage();
                responseMessage.setRole(Message.ROLE_ASSISTANT);
                responseMessage.setSuccess(true);

                // 提取内容
                JSONArray contentArray = object.getJSONArray("content");
                if (contentArray != null && !contentArray.isEmpty()) {
                    StringBuilder contentBuilder = new StringBuilder();
                    for (int i = 0; i < contentArray.size(); i++) {
                        JSONObject contentItem = contentArray.getJSONObject(i);
                        String type = contentItem.getString("type");
                        if ("text".equals(type)) {
                            contentBuilder.append(contentItem.getString("text"));
                        }
                    }
                    responseMessage.setContent(contentBuilder.toString());
                }

                // 提取使用统计
                JSONObject usage = object.getJSONObject("usage");
                if (usage != null) {
                    Usage usageObj = new Usage();
                    usageObj.setPromptTokens(usage.getInteger("input_tokens") != null ? usage.getInteger("input_tokens") : 0);
                    usageObj.setCompletionTokens(usage.getInteger("output_tokens") != null ? usage.getInteger("output_tokens") : 0);
                    responseMessage.setUsage(usageObj);
                }

                callback.accept(responseMessage);
            });
        } else {
            post = buildOpenAIRequest(messages, false);
            post.onSuccess(response -> {
                if (response.statusCode() != 200) {
                    ResponseMessage responseMessage = new ResponseMessage();
                    responseMessage.setRole(Message.ROLE_ASSISTANT);
                    responseMessage.setError(response.body());
                    responseMessage.setSuccess(false);
                    callback.accept(responseMessage);
                    return;
                }
                ChatWholeResponse chatResponse = JSON.parseObject(response.body(), ChatWholeResponse.class);
                ResponseMessage responseMessage = chatResponse.getChoice().getMessage();
                responseMessage.setUsage(chatResponse.getUsage());
                responseMessage.setPromptLogprobs(chatResponse.getPromptLogprobs());
                responseMessage.setSuccess(true);
                callback.accept(responseMessage);
            });
        }
        post.onFailure(Throwable::printStackTrace).submit();
    }

    /**
     * 构建 OpenAI 格式的请求
     */
    private HttpPost buildOpenAIRequest(List<Message> messages, boolean stream) {
        JSONObject jsonObject = new JSONObject();
        jsonObject.put("model", options.getModel());
        jsonObject.put("stream", stream);
        jsonObject.put("messages", messages);

        if (options.getTemperature() != null) {
            jsonObject.put("temperature", options.getTemperature());
        }

        List<Tool> toolList = new ArrayList<>();
        options.functions().forEach((tool, function) -> {
            Tool t = new Tool();
            t.setType("function");
            t.setFunction(function);
            toolList.add(t);
        });
        if (!toolList.isEmpty()) {
            jsonObject.put("tools", toolList);
            jsonObject.put("tool_choice", "auto");
        }

        // 合并 extraBody 参数到请求 JSON
        JSONObject extraBody = options.getExtraBody();
        if (extraBody != null && !extraBody.isEmpty()) {
            jsonObject.putAll(extraBody);
        }

        HttpPost post = Feat.postJson(options.baseUrl() + "/chat/completions", opts -> {
            opts.debug(options.isDebug());
        }, header -> {
            if (FeatUtils.isNotBlank(options.apiKey())) {
                header.add(HeaderName.AUTHORIZATION, "Bearer " + options.apiKey());
            }
            options.getHeaders().forEach(header::add);
        }, jsonObject);
        post.onFailure(throwable -> throwable.printStackTrace());
        return post;
    }

    /**
     * 构建 Anthropic 格式的请求
     */
    private HttpPost buildAnthropicRequest(List<Message> messages, boolean stream) {
        JSONObject jsonObject = new JSONObject();
        jsonObject.put("model", options.getModel());
        jsonObject.put("stream", stream);

        // Anthropic 使用 max_tokens 而不是 temperature 来控制
        // 设置默认的 max_tokens
        jsonObject.put("max_tokens", 4096);

        // 处理系统消息（Anthropic 将 system 作为顶级字段）
        String systemContent = options.getSystem();
        List<Message> userMessages = new ArrayList<>();
        for (Message message : messages) {
            if (Message.ROLE_SYSTEM.equals(message.getRole())) {
                if (systemContent == null) {
                    systemContent = message.getContent();
                }
            } else {
                userMessages.add(message);
            }
        }

        if (systemContent != null) {
            jsonObject.put("system", systemContent);
        }

        // 设置消息（Anthropic 不支持 system 角色在 messages 中）
        if (!userMessages.isEmpty()) {
            jsonObject.put("messages", userMessages);
        }

        // 处理工具
        if (!options.functions().isEmpty()) {
            JSONArray toolsArray = new JSONArray();
            options.functions().forEach((toolName, tool) -> {
                JSONObject toolJson = new JSONObject();
                toolJson.put("name", tool);
                toolJson.put("description", options.functions().get(tool).getDescription());
                toolJson.put("input_schema", options.functions().get(tool).getParameters());
                toolsArray.add(toolJson);
            });
            jsonObject.put("tools", toolsArray);
        }

        // Anthropic API 端点
        String url = options.baseUrl() + "/v1/messages";

        HttpPost post = Feat.postJson(url, opts -> {
            opts.debug(options.isDebug());
        }, header -> {
            if (FeatUtils.isNotBlank(options.apiKey())) {
                header.add("x-api-key", options.apiKey());
            }
            header.add("anthropic-version", ANTHROPIC_VERSION);
            options.getHeaders().forEach(header::add);
        }, jsonObject);
        post.onFailure(throwable -> throwable.printStackTrace());
        return post;
    }


    /**
     * 发送非流式聊天请求（无工具版本）
     *
     * @param content  用户输入内容
     * @param callback 响应回调
     */
    public void chat(String content, Consumer<ResponseMessage> callback) {
        chat(Collections.singletonList(Message.ofUser(content)), callback);
    }

    /**
     * 获取聊天选项配置
     *
     * @return 聊天选项配置
     */
    public ChatOptions getOptions() {
        return options;
    }
}