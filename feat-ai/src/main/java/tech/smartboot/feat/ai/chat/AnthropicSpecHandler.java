package tech.smartboot.feat.ai.chat;

import com.alibaba.fastjson2.JSON;
import com.alibaba.fastjson2.JSONArray;
import com.alibaba.fastjson2.JSONObject;
import tech.smartboot.feat.Feat;
import tech.smartboot.feat.ai.chat.entity.Message;
import tech.smartboot.feat.ai.chat.entity.ResponseMessage;
import tech.smartboot.feat.ai.chat.entity.StreamResponseCallback;
import tech.smartboot.feat.ai.chat.entity.Usage;
import tech.smartboot.feat.core.client.HttpPost;
import tech.smartboot.feat.core.common.FeatUtils;
import tech.smartboot.feat.core.common.exception.FeatException;
import tech.smartboot.feat.core.common.logging.Logger;
import tech.smartboot.feat.core.common.logging.LoggerFactory;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.Consumer;

/**
 * Anthropic API 处理器实现
 */
public class AnthropicSpecHandler extends SpecHandler {
    private static final Logger LOGGER = LoggerFactory.getLogger(AnthropicSpecHandler.class);
    private static final String ANTHROPIC_VERSION = "2023-06-01";

    public AnthropicSpecHandler(ChatOptions options) {
        super(options);
    }


    private HttpPost buildRequest(List<Message> messages, boolean stream) {
        JSONObject jsonObject = new JSONObject();
        jsonObject.put("model", options.getModel());
        jsonObject.put("stream", stream);
        jsonObject.put("max_tokens", 4096);

        // 处理系统消息
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

        String url = options.baseUrl() + "/v1/messages";

        return Feat.postJson(url, opts -> {
            opts.debug(options.isDebug());
        }, header -> {
            if (FeatUtils.isNotBlank(options.apiKey())) {
                header.add("x-api-key", options.apiKey());
            }
            header.add("anthropic-version", ANTHROPIC_VERSION);
            options.getHeaders().forEach(header::add);
        }, jsonObject);
    }

    @Override
    public void chatStream(List<Message> messages, StreamResponseCallback consumer) {
        HttpPost post = buildRequest(messages, true);
        StringBuilder contentBuilder = new StringBuilder();
        StringBuilder reasoningBuilder = new StringBuilder();
        AtomicInteger status = new AtomicInteger(STREAM_STATUS_INIT);
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
                        break;
                    case "content_block_start":
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
                        break;
                    case "message_delta":
                        break;
                    case "message_stop":
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
                })
                .onFailure(Throwable::printStackTrace)
                .submit();
    }
}
