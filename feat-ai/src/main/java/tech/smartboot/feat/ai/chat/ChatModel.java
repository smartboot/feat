/*
 *  Copyright (C) [2024] smartboot [zhengjunweimail@163.com]
 *
 *  企业用户未经smartboot组织特别许可，需遵循AGPL-3.0开源协议合理合法使用本项目。
 *
 *   Enterprise users are required to use this project reasonably
 *   and legally in accordance with the AGPL-3.0 open source agreement
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
import tech.smartboot.feat.ai.chat.prompt.Prompt;
import tech.smartboot.feat.core.client.HttpPost;
import tech.smartboot.feat.core.common.FeatUtils;
import tech.smartboot.feat.core.common.HeaderName;
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
        chatStream(Collections.singletonList(Message.ofUser(content)), Collections.emptyList(), consumer);
    }

    /**
     * 发送流式聊天请求（消息列表版本）
     *
     * @param messages 消息列表
     * @param consumer 流式响应回调
     */
    public void chatStream(List<Message> messages, StreamResponseCallback consumer) {
        chatStream(messages, Collections.emptyList(), consumer);
    }

    /**
     * 发送流式聊天请求（完整版本）
     *
     * @param messages 消息列表
     * @param tools    工具列表
     * @param consumer 流式响应回调
     */
    public void chatStream(List<Message> messages, List<String> tools, StreamResponseCallback consumer) {
        HttpPost post = chat0(messages, tools, true);
        StringBuilder contentBuilder = new StringBuilder();
        StringBuilder reasoningBuilder = new StringBuilder();
        Map<Integer, ToolCall> toolCallMap = new HashMap<>();
        AtomicInteger status = new AtomicInteger(STREAM_STATUS_INIT);
        post.onSuccess(response -> {
            //若sse升级成功，则忽略onSuccess逻辑
            if (status.get() == STREAM_STATUS_INIT) {
                ResponseMessage responseMessage = new ResponseMessage();
                responseMessage.setRole(Message.ROLE_ASSISTANT);
                responseMessage.setError(response.body());
                responseMessage.setSuccess(false);
                consumer.onCompletion(responseMessage);
            }
        }).onFailure(throwable -> {
            ResponseMessage responseMessage = new ResponseMessage();
            responseMessage.setRole(Message.ROLE_ASSISTANT);
            responseMessage.setError(throwable.getMessage());
            responseMessage.setSuccess(false);
            consumer.onCompletion(responseMessage);
        }).onSSE(sse -> sse.onData(event -> {
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
        })).submit();
    }

    /**
     * 发送流式聊天请求（工具版本）
     *
     * @param chat     用户输入内容
     * @param tools    工具列表
     * @param consumer 流式响应回调
     */
    public void chatStream(String chat, List<String> tools, StreamResponseCallback consumer) {
        chatStream(Collections.singletonList(Message.ofUser(chat)), tools, consumer);
    }

    /**
     * 发送非流式聊天请求（简单版本）
     *
     * @param content 用户输入内容
     * @return 包含响应消息的CompletableFuture
     */
    public CompletableFuture<ResponseMessage> chat(String content) {
        return chat(content, Collections.emptyList());
    }

    /**
     * 发送非流式聊天请求（消息列表版本）
     *
     * @param messages 消息列表
     * @return 包含响应消息的CompletableFuture
     */
    public CompletableFuture<ResponseMessage> chat(List<Message> messages) {
        CompletableFuture<ResponseMessage> future = new CompletableFuture<>();
        chat(messages, Collections.emptyList(), future::complete);
        return future;
    }

    /**
     * 发送非流式聊天请求（工具版本）
     *
     * @param content 用户输入内容
     * @param tools   工具列表
     * @return 包含响应消息的CompletableFuture
     */
    public CompletableFuture<ResponseMessage> chat(String content, List<String> tools) {
        CompletableFuture<ResponseMessage> future = new CompletableFuture<>();
        chat(content, tools, future::complete);
        return future;
    }

    /**
     * 发送非流式聊天请求（回调版本）
     *
     * @param messages 消息列表
     * @param tools    工具列表
     * @param callback 响应回调
     */
    public void chat(List<Message> messages, List<String> tools, Consumer<ResponseMessage> callback) {
        HttpPost post = chat0(messages, tools, false);

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
        }).onFailure(Throwable::printStackTrace).submit();
    }

    /**
     * 发送非流式聊天请求（工具+回调版本）
     *
     * @param content  用户输入内容
     * @param tools    工具列表
     * @param callback 响应回调
     */
    public void chat(String content, List<String> tools, Consumer<ResponseMessage> callback) {
        chat(Collections.singletonList(Message.ofUser(content)), tools, callback);
    }

    /**
     * 构建并发送聊天请求的核心方法
     *
     * @param messages 消息列表
     * @param tools    工具列表
     * @param stream   是否使用流式响应
     * @return HttpPost请求对象
     */
    private HttpPost chat0(List<Message> messages, List<String> tools, boolean stream) {
        JSONObject jsonObject = new JSONObject();
        jsonObject.put("model", options.getModel().model());
        jsonObject.put("stream", stream);
        jsonObject.put("messages", messages);
        if (options.responseFormat() != null) {
            jsonObject.put("response_format", JSONObject.of("type", options.responseFormat().getType()));
        }

        List<Tool> toolList = new ArrayList<>();
        for (String tool : tools) {
            if (!options.functions().containsKey(tool)) {
                throw new RuntimeException("工具 " + tool + " 不存在");
            }
            Tool t = new Tool();
            t.setType("function");
            t.setFunction(options.functions().get(tool));
            toolList.add(t);
        }
        if (!toolList.isEmpty()) {
            if (options.getModel().hasCapability(ChatModelVendor.CAPABILITY_FUNCTION_CALL)) {
                jsonObject.put("tools", toolList);
                jsonObject.put("tool_choice", "auto");
            } else {
                LOGGER.warn("current model:{} unSupport function call.", options.getModel().model());
            }
        }
        if (options.getModel().getPreRequest() != null) {
            options.getModel().getPreRequest().preRequest(this, options.getModel(), jsonObject);
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
     * 发送非流式聊天请求（单工具版本）
     *
     * @param content  用户输入内容
     * @param tool     工具名称
     * @param callback 响应回调
     */
    public void chat(String content, String tool, Consumer<ResponseMessage> callback) {
        chat(content, Collections.singletonList(tool), callback);
    }

    /**
     * 发送非流式聊天请求（无工具版本）
     *
     * @param content  用户输入内容
     * @param callback 响应回调
     */
    public void chat(String content, Consumer<ResponseMessage> callback) {
        chat(content, Collections.emptyList(), callback);
    }

    /**
     * 发送非流式聊天请求（提示词模板版本）
     *
     * @param prompt 提示词模板
     * @param data   模板数据
     * @return 包含响应消息的CompletableFuture
     */
    public CompletableFuture<ResponseMessage> chat(Prompt prompt, Consumer<Map<String, String>> data) {
        CompletableFuture<ResponseMessage> future = new CompletableFuture<>();
        chat(prompt, data, future::complete);
        return future;
    }

    /**
     * 发送非流式聊天请求（提示词模板+回调版本）
     *
     * @param prompt   提示词模板
     * @param data     模板数据
     * @param callback 响应回调
     */
    public void chat(Prompt prompt, Consumer<Map<String, String>> data, Consumer<ResponseMessage> callback) {
        Map<String, String> params = new HashMap<>();
        data.accept(params);
        chat(prompt.prompt(params), callback);
    }

    /**
     * 发送流式聊天请求（提示词模板版本）
     *
     * @param prompt   提示词模板
     * @param data     模板数据
     * @param callback 流式响应回调
     */
    public void chatStream(Prompt prompt, Consumer<Map<String, String>> data, StreamResponseCallback callback) {
        Map<String, String> params = new HashMap<>();
        data.accept(params);
        chatStream(prompt.prompt(params), callback);
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