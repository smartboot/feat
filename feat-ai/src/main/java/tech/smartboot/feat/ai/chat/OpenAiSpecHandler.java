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
import tech.smartboot.feat.core.client.HttpPost;
import tech.smartboot.feat.core.common.FeatUtils;
import tech.smartboot.feat.core.common.HeaderName;
import tech.smartboot.feat.core.common.exception.FeatException;
import tech.smartboot.feat.core.common.logging.Logger;
import tech.smartboot.feat.core.common.logging.LoggerFactory;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.Consumer;

/**
 * OpenAI API 处理器实现
 */
public class OpenAiSpecHandler extends SpecHandler {
    private static final Logger LOGGER = LoggerFactory.getLogger(OpenAiSpecHandler.class);

    public OpenAiSpecHandler(ChatOptions options) {
        super(options);
    }

    private HttpPost buildRequest(List<Message> messages, boolean stream) {
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

        JSONObject extraBody = options.getExtraBody();
        if (extraBody != null && !extraBody.isEmpty()) {
            jsonObject.putAll(extraBody);
        }

        return Feat.postJson(options.baseUrl() + "/chat/completions", opts -> {
            opts.debug(options.isDebug());
        }, header -> {
            if (FeatUtils.isNotBlank(options.apiKey())) {
                header.add(HeaderName.AUTHORIZATION, "Bearer " + options.apiKey());
            }
            options.getHeaders().forEach(header::add);
        }, jsonObject);
    }

    @Override
    public void chatStream(List<Message> messages, StreamResponseCallback consumer) {
        HttpPost post = buildRequest(messages, true);
        Map<Integer, ToolCall> toolCallMap = new HashMap<>();
        StringBuilder contentBuilder = new StringBuilder();
        StringBuilder reasoningBuilder = new StringBuilder();
        AtomicInteger status = new AtomicInteger(STREAM_STATUS_INIT);
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
        })).onSuccess(response -> {
            if (status.get() == SpecHandler.STREAM_STATUS_INIT) {
                status.set(SpecHandler.STREAM_STATUS_ERROR);
                consumer.onError(new FeatException(response.body()));
            }
        }).onFailure(throwable -> {
            status.set(SpecHandler.STREAM_STATUS_ERROR);
            consumer.onError(throwable);
        }).submit();
    }

    @Override
    public void chat(List<Message> messages, Consumer<ResponseMessage> callback) {
        HttpPost post = buildRequest(messages, false);
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
                })
                .onFailure(Throwable::printStackTrace)
                .submit();
    }
}
