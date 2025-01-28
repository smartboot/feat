package tech.smartboot.feat.ai.chat;

import com.alibaba.fastjson2.JSON;
import tech.smartboot.feat.ai.Options;
import tech.smartboot.feat.core.client.HttpClient;
import tech.smartboot.feat.core.common.enums.HeaderNameEnum;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Consumer;

public class ChatModel {
    private Options options;
    private final List<Message> history = new ArrayList<>();
    private ChatResponse chatResponse;

    public ChatModel(Options options) {
        if (options.baseUrl().endsWith("/")) {
            options.baseUrl(options.baseUrl().substring(0, options.baseUrl().length() - 1));
        }
        this.options = options;
    }

    public ChatResponse getChatResponse() {
        return chatResponse;
    }

    public String getResponse() {
        return "Gitee AI: " + chatResponse.getChoices().get(0).getMessage().getContent();
    }

    public List<Message> getHistory() {
        return history;
    }

    public void chat(String content, Consumer<ChatModel> consumer) {
        System.out.println("我：" + content);
        ChatRequest request = new ChatRequest();
        request.setModel(options.getModel());
        request.setStream(false);
        Message message = new Message();
        message.setContent(content);
        message.setRole("user");
        history.add(message);
        request.setMessages(history);

        HttpClient httpClient = new HttpClient(options.baseUrl() + "/chat/completions");
        httpClient.configuration().debug(false);
        httpClient.post().header().setContentType("application/json")
                .add(HeaderNameEnum.AUTHORIZATION.getName(), "Bearer " + options.getApiKey())
                .done()
                .body()
                .write(JSON.toJSONBytes(request)).done().onSuccess(response -> {
                    chatResponse = JSON.parseObject(response.body(), ChatResponse.class);
                    chatResponse.getChoices().forEach(choice -> {
                        history.add(choice.getMessage());
                    });
                    consumer.accept(this);
                }).onFailure(throwable -> throwable.printStackTrace()).done();
    }


}
