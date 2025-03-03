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
import tech.smartboot.feat.Feat;
import tech.smartboot.feat.ai.ModelMeta;
import tech.smartboot.feat.ai.Options;
import tech.smartboot.feat.ai.chat.entity.ChatRequest;
import tech.smartboot.feat.ai.chat.entity.ChatStreamResponse;
import tech.smartboot.feat.ai.chat.entity.ChatWholeResponse;
import tech.smartboot.feat.ai.chat.entity.Message;
import tech.smartboot.feat.ai.chat.entity.ResponseCallback;
import tech.smartboot.feat.ai.chat.entity.ResponseMessage;
import tech.smartboot.feat.ai.chat.entity.StreamChoice;
import tech.smartboot.feat.ai.chat.entity.StreamResponseCallback;
import tech.smartboot.feat.ai.chat.entity.Tool;
import tech.smartboot.feat.ai.chat.entity.ToolCall;
import tech.smartboot.feat.ai.prompt.Prompt;
import tech.smartboot.feat.core.client.HttpPost;
import tech.smartboot.feat.core.client.HttpResponse;
import tech.smartboot.feat.core.client.stream.ServerSentEventStream;
import tech.smartboot.feat.core.client.stream.Stream;
import tech.smartboot.feat.core.common.enums.HeaderNameEnum;
import tech.smartboot.feat.core.common.utils.StringUtils;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.function.Consumer;

public class ChatModel {
    private final Options options;
    private final List<Message> history = new ArrayList<>();

    public ChatModel(Options options) {
        if (options.baseUrl().endsWith("/")) {
            options.baseUrl(options.baseUrl().substring(0, options.baseUrl().length() - 1));
        }
        this.options = options;
        if (StringUtils.isNotBlank(options.getSystem())) {
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

    public void chatStream(String content, List<String> tools, StreamResponseCallback consumer) {
        HttpPost post = chat0(content, tools, true);
        StringBuilder contentBuilder = new StringBuilder();
        Map<Integer, ToolCall> toolCallMap = new HashMap<>();

        post.onResponseHeader(resp -> {
            if (resp.statusCode() == 200) {
                post.onResponseBody(new ServerSentEventStream() {
                    @Override
                    public void onEvent(HttpResponse httpResponse, Map<String, String> event) {
                        String data = event.get(ServerSentEventStream.DATA);
                        if ("[DONE]".equals(data) || StringUtils.isBlank(data)) {
                            ResponseMessage responseMessage = new ResponseMessage();
                            responseMessage.setRole(Message.ROLE_ASSISTANT);
                            responseMessage.setContent(contentBuilder.toString());
                            responseMessage.setToolCalls(toolCallMap.values());
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
                        ChatStreamResponse object = JSON.parseObject(data, ChatStreamResponse.class);
                        StreamChoice choice = object.getChoice();
                        if (choice.getDelta().getContent() != null) {
                            consumer.onStreamResponse(choice.getDelta().getContent());
                            contentBuilder.append(choice.getDelta().getContent());
                        }
                        if (choice.getDelta().getToolCalls() != null) {
                            for (ToolCall toolCall : choice.getDelta().getToolCalls()) {
                                ToolCall tool = toolCallMap.computeIfAbsent(toolCall.getIndex(), k -> {
                                    ToolCall t = new ToolCall();
                                    t.setFunction(new HashMap<>());
                                    return t;
                                });
                                if (StringUtils.isNotBlank(toolCall.getId())) {
                                    tool.setId(toolCall.getId());
                                }
                                if (StringUtils.isNotBlank(toolCall.getType())) {
                                    tool.setType(toolCall.getType());
                                }
                                if (toolCall.getFunction() != null) {
                                    toolCall.getFunction().forEach((k, v) -> {
                                        String preV = tool.getFunction().get(k);
                                        if (StringUtils.isNotBlank(preV)) {
                                            tool.getFunction().put(k, preV + v);
                                        } else {
                                            tool.getFunction().put(k, v);
                                        }
                                    });
                                }
                            }
                        }
                    }
                });
            } else {
                post.onResponseBody(new Stream() {
                    final ByteArrayOutputStream sb = new ByteArrayOutputStream();

                    @Override
                    public void stream(HttpResponse response, byte[] bytes, boolean end) throws IOException {
                        sb.write(bytes);
                        if (end) {
                            ResponseMessage responseMessage = new ResponseMessage();
                            responseMessage.setRole(Message.ROLE_ASSISTANT);
                            responseMessage.setError(sb.toString());
                            responseMessage.setSuccess(false);
                            consumer.onCompletion(responseMessage);
                        }
                    }

                });
            }
        });

    }

    public CompletableFuture<ResponseMessage> chat(String content) {
        return chat(content, Collections.emptyList());
    }

    public CompletableFuture<ResponseMessage> chat(String content, List<String> tools) {
        CompletableFuture<ResponseMessage> future = new CompletableFuture<>();
        chat(content, tools, future::complete);
        return future;
    }

    public void chat(String content, List<String> tools, ResponseCallback callback) {
        HttpPost post = chat0(content, tools, false);

        post.onSuccess(response -> {
            if (response.statusCode() != 200) {
                ResponseMessage responseMessage = new ResponseMessage();
                responseMessage.setRole(Message.ROLE_ASSISTANT);
                responseMessage.setError(response.body());
                responseMessage.setSuccess(false);
                callback.onCompletion(responseMessage);
                return;
            }
            ChatWholeResponse chatResponse = JSON.parseObject(response.body(), ChatWholeResponse.class);
            chatResponse.getChoices().forEach(choice -> history.add(choice.getMessage()));
            ResponseMessage responseMessage = chatResponse.getChoice().getMessage();
            responseMessage.setUsage(chatResponse.getUsage());
            responseMessage.setPromptLogprobs(chatResponse.getPromptLogprobs());
            responseMessage.setSuccess(true);
            callback.onCompletion(responseMessage);
        });
    }


    private HttpPost chat0(String content, List<String> tools, boolean stream) {
        ModelMeta modelMeta = ModelMeta.get(options.baseUrl(), options.getModel());
        if (modelMeta != null && !modelMeta.isToolSupport() && !tools.isEmpty()) {
            if (options.isIgnoreUnSupportedTool()) {
                tools.clear();
            } else {
                throw new RuntimeException("模型 " + options.getModel() + " 不支持工具");
            }
        }
        System.out.println("我：" + content);
        ChatRequest request = new ChatRequest();
        request.setModel(options.getModel());
        request.setStream(stream);
        Message message = new Message();
        message.setContent(content);
        message.setRole("user");
        history.add(message);
        request.setMessages(history);

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
            request.setTools(toolList);
            request.setToolChoice("auto");
        }


        HttpPost post = Feat.postJson(options.baseUrl() + "/chat/completions", opts -> {
            opts.debug(options.isDebug());
        }, header -> {
            if (StringUtils.isNotBlank(options.getApiKey())) {
                header.add(HeaderNameEnum.AUTHORIZATION.getName(), "Bearer " + options.getApiKey());
            }
            options.getHeaders().forEach(header::add);
        }, request);
        post.onFailure(throwable -> throwable.printStackTrace()).submit();
        return post;
    }

    public void chat(String content, String tool, ResponseCallback callback) {
        chat(content, Collections.singletonList(tool), callback);
    }

    public void chat(String content, ResponseCallback callback) {
        chat(content, Collections.emptyList(), callback);
    }

    public CompletableFuture<ResponseMessage> chat(Prompt prompt, Consumer<Map<String, String>> data) {
        CompletableFuture<ResponseMessage> future = new CompletableFuture<>();
        chat(prompt, data, future::complete);
        return future;
    }

    public void chat(Prompt prompt, Consumer<Map<String, String>> data, ResponseCallback callback) {
        Map<String, String> params = new HashMap<>();
        data.accept(params);
        history.clear();
        if (StringUtils.isNotBlank(prompt.role())) {
            options.system(prompt.role());
            Message message = new Message();
            message.setRole(Message.ROLE_SYSTEM);
            message.setContent(options.getSystem());
            history.add(message);
        }
        chat(prompt.prompt(params), callback);
    }

    public void chatStream(Prompt prompt, Consumer<Map<String, String>> data, StreamResponseCallback callback) {
        Map<String, String> params = new HashMap<>();
        data.accept(params);
        history.clear();
        if (StringUtils.isNotBlank(prompt.role())) {
            options.system(prompt.role());
            Message message = new Message();
            message.setRole(Message.ROLE_SYSTEM);
            message.setContent(options.getSystem());
            history.add(message);
        }
        chatStream(prompt.prompt(params), callback);
    }

}
