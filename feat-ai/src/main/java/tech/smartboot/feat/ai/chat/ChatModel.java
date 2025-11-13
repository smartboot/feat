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
import java.util.function.Consumer;

/**
 * @author 三刀 zhengjunweimail@163.com
 * @version v1.0.0
 */
public class ChatModel {
    private static final Logger LOGGER = LoggerFactory.getLogger(ChatModel.class);
    private final ChatOptions options;
    private final List<Message> history = new ArrayList<>();

    public ChatModel(ChatOptions options) {
        if (options.baseUrl().endsWith("/")) {
            options.baseUrl(options.baseUrl().substring(0, options.baseUrl().length() - 1));
        }
        this.options = options;
        if (FeatUtils.isNotBlank(options.getSystem())) {
            Message message = new Message();
            message.setRole(Message.ROLE_SYSTEM);
            message.setContent(options.getSystem());
            history.add(message);
        }
    }

    public List<Message> getHistory() {
        return history;
    }

    public void chatStream(String content, StreamResponseCallback consumer) {
        chatStream(content, Collections.emptyList(), consumer);
    }

    public void chatStream(String chat, List<String> tools, StreamResponseCallback consumer) {
        HttpPost post = chat0(chat, tools, true);
        StringBuilder contentBuilder = new StringBuilder();
        Map<Integer, ToolCall> toolCallMap = new HashMap<>();

        post.onSuccess(response -> {
                    ResponseMessage responseMessage = new ResponseMessage();
                    responseMessage.setRole(Message.ROLE_ASSISTANT);
                    responseMessage.setError(response.body());
                    responseMessage.setSuccess(false);
                    consumer.onCompletion(responseMessage);
                })
                .onSSE(sse -> sse.onData(event -> {
                    String data = event.getData();
                    if ("[DONE]".equals(data) || FeatUtils.isBlank(data)) {
                        ResponseMessage responseMessage = new ResponseMessage();
                        responseMessage.setRole(Message.ROLE_ASSISTANT);
                        responseMessage.setContent(contentBuilder.toString());
                        responseMessage.setToolCalls(new ArrayList<>(toolCallMap.values()));
                        responseMessage.setSuccess(true);
                        consumer.onCompletion(responseMessage);
                        if (!responseMessage.isDiscard()) {
                            Message message = new Message();
                            message.setRole(Message.ROLE_ASSISTANT);
                            message.setContent(contentBuilder.toString());
                            history.add(message);
                        }
                        return;
                    }
                    JSONObject object = JSON.parseObject(data);
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
                        consumer.onStreamResponse(reasoningContent);
                        contentBuilder.append(reasoningContent);
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

    public CompletableFuture<ResponseMessage> chat(String content) {
        return chat(content, Collections.emptyList());
    }

    public CompletableFuture<ResponseMessage> chat(String content, List<String> tools) {
        CompletableFuture<ResponseMessage> future = new CompletableFuture<>();
        chat(content, tools, future::complete);
        return future;
    }

    public void chat(String content, List<String> tools, Consumer<ResponseMessage> callback) {
        HttpPost post = chat0(content, tools, false);

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
            chatResponse.getChoices().forEach(choice -> history.add(choice.getMessage()));
            ResponseMessage responseMessage = chatResponse.getChoice().getMessage();
            responseMessage.setUsage(chatResponse.getUsage());
            responseMessage.setPromptLogprobs(chatResponse.getPromptLogprobs());
            responseMessage.setSuccess(true);
            callback.accept(responseMessage);
        }).onFailure(Throwable::printStackTrace).submit();
    }


    private HttpPost chat0(String content, List<String> tools, boolean stream) {
        LOGGER.warn("content:");
        LOGGER.warn(content);
//        System.out.println("我：" + content);
        JSONObject jsonObject = new JSONObject();
        jsonObject.put("model", options.getModel().model());
        jsonObject.put("stream", stream);
        Message message = new Message();
        message.setContent(content);
        message.setRole("user");
        history.add(message);
        jsonObject.put("messages", history);
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

    public void chat(String content, String tool, Consumer<ResponseMessage> callback) {
        chat(content, Collections.singletonList(tool), callback);
    }

    public void chat(String content, Consumer<ResponseMessage> callback) {
        chat(content, Collections.emptyList(), callback);
    }

    public CompletableFuture<ResponseMessage> chat(Prompt prompt, Consumer<Map<String, String>> data) {
        CompletableFuture<ResponseMessage> future = new CompletableFuture<>();
        chat(prompt, data, future::complete);
        return future;
    }

    public void chat(Prompt prompt, Consumer<Map<String, String>> data, Consumer<ResponseMessage> callback) {
        Map<String, String> params = new HashMap<>();
        data.accept(params);
        history.clear();
        chat(prompt.prompt(params), callback);
    }

    public void chatStream(Prompt prompt, Consumer<Map<String, String>> data, StreamResponseCallback callback) {
        Map<String, String> params = new HashMap<>();
        data.accept(params);
        history.clear();
        chatStream(prompt.prompt(params), callback);
    }

    public ChatOptions getOptions() {
        return options;
    }
}
