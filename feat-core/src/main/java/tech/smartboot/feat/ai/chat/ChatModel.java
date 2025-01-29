package tech.smartboot.feat.ai.chat;

import com.alibaba.fastjson2.JSON;
import tech.smartboot.feat.ai.Options;
import tech.smartboot.feat.core.client.HttpClient;
import tech.smartboot.feat.core.common.enums.HeaderNameEnum;
import tech.smartboot.feat.core.common.utils.StringUtils;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
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
        return "Gitee AI: " + chatResponse.getChoices().get(0).getMessage().getContent();
    }

    public List<Message> getHistory() {
        return history;
    }

    public void chat(String content, List<String> tools, Consumer<ChatModel> consumer) {
        System.out.println("我：" + content);
        ChatRequest request = new ChatRequest();
        request.setModel(options.getModel());
        request.setStream(false);
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


        HttpClient httpClient = new HttpClient(options.baseUrl() + "/chat/completions");
        httpClient.configuration().debug(true);
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

    public void chat(String content, String tool, Consumer<ChatModel> consumer) {
        chat(content, Collections.singletonList(tool), consumer);
    }

    public void chat(String content, Consumer<ChatModel> consumer) {
        chat(content, Collections.emptyList(), consumer);
    }

}
