package tech.smartboot.feat.ai.chat;

import com.alibaba.fastjson2.JSON;
import tech.smartboot.feat.Feat;
import tech.smartboot.feat.ai.Options;
import tech.smartboot.feat.core.client.HttpPost;
import tech.smartboot.feat.core.client.HttpResponse;
import tech.smartboot.feat.core.client.stream.ServerSentEventStream;
import tech.smartboot.feat.core.client.stream.Stream;
import tech.smartboot.feat.core.common.enums.HeaderNameEnum;
import tech.smartboot.feat.core.common.utils.StringUtils;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.function.Consumer;

public class ChatModel {
    private final Options options;
    private final List<Message> history = new ArrayList<>();
    private ChatResponse chatResponse;

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

    public ChatResponse getChatResponse() {
        return chatResponse;
    }

    public String getResponse() {
        return "Gitee AI: " + chatResponse.getChoice().getMessage().getContent();
    }

    public List<Message> getHistory() {
        return history;
    }

    public void chatStream(String content, Consumer<ChatModel> consumer) {
        chatStream(content, Collections.emptyList(), consumer);
    }

    public void chatStream(String content, List<String> tools, Consumer<ChatModel> consumer) {
        HttpPost post = chat0(content, tools, true);
        post.onResponseHeader(resp -> {
            if (resp.statusCode() == 200) {
                post.onResponseBody(new ServerSentEventStream() {
                    @Override
                    public void onEvent(HttpResponse httpResponse, Map<String, String> event) {
                        String data = event.get(ServerSentEventStream.DATA);
                        if ("[DONE]".equals(data) || StringUtils.isBlank(data)) {
                            return;
                        }
                        ChatResponse object = JSON.parseObject(data, ChatResponse.class);
                        Choice choice = object.getChoice();
                        if (choice.getDelta().getContent() == null) {
                            choice.getDelta().setContent("");
                        }
                        if (chatResponse == null || !chatResponse.getId().equals(object.getId())) {
                            chatResponse = object;
                            ResponseMessage responseMessage = new ResponseMessage();
                            responseMessage.setRole(choice.getDelta().getRole());
                            responseMessage.setContent(choice.getDelta().getContent());
                            choice.setMessage(responseMessage);
                            history.add(responseMessage);
                        } else {
                            Message deltaMessage = new Message();
                            deltaMessage.setContent(choice.getDelta().getContent());
                            chatResponse.getChoice().setDelta(deltaMessage);
                            chatResponse.getChoice().setStopReason(choice.getStopReason());
                            chatResponse.getChoice().setFinishReason(choice.getFinishReason());
                            ResponseMessage fullMessage = chatResponse.getChoice().getMessage();
                            fullMessage.setContent(fullMessage.getContent() + choice.getDelta().getContent());
                        }

                        consumer.accept(ChatModel.this);
                    }
                });
            } else {
                post.onResponseBody(new Stream() {
                    @Override
                    public void stream(HttpResponse response, byte[] bytes, boolean end) throws IOException {
                        System.out.print(new String(bytes));
                    }
                });
            }
        });

    }

    public void chat(String content, List<String> tools, Consumer<ChatModel> consumer) {
        HttpPost post = chat0(content, tools, false);
        post.onSuccess(response -> {
            chatResponse = JSON.parseObject(response.body(), ChatResponse.class);
            chatResponse.getChoices().forEach(choice -> {
                history.add(choice.getMessage());
            });
            consumer.accept(this);
        });
    }


    private HttpPost chat0(String content, List<String> tools, boolean stream) {
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
        request.setTools(toolList);


        HttpPost post = Feat.postJson(options.baseUrl() + "/chat/completions", opts -> {
            opts.debug(options.isDebug());
        }, header -> {
            header.add(HeaderNameEnum.AUTHORIZATION.getName(), "Bearer " + options.getApiKey());
        }, request);
        post.onFailure(throwable -> throwable.printStackTrace()).submit();
        return post;
    }

    public void chat(String content, String tool, Consumer<ChatModel> consumer) {
        chat(content, Collections.singletonList(tool), consumer);
    }

    public void chat(String content, Consumer<ChatModel> consumer) {
        chat(content, Collections.emptyList(), consumer);
    }

}
