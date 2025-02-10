package tech.smartboot.feat.ai.chat.entity;

import com.alibaba.fastjson2.JSON;
import com.alibaba.fastjson2.annotation.JSONField;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Consumer;

public class ChatWholeResponse extends ChatResponse {

    /**
     * 是一个包含一个或多个聊天响应的列表。如果请求的参数 n 大于 1时(请求模型生成多个答复)，列表的元素将是多个。
     * 一般情况下 choices 只包含一个元素
     */
    private List<WholeChoice> choices;
    private Usage usage;
    @JSONField(name = "prompt_logprobs")
    private String promptLogprobs;

    public WholeChoice getChoice() {
        return choices.get(0);
    }

    public List<WholeChoice> getChoices() {
        return choices;
    }

    public void setChoices(List<WholeChoice> choices) {
        this.choices = choices;
    }

    public <T> boolean ifMatchTool(String functionName, Class<T> clazz, Consumer<T> consumer) {
        List<T> result = new ArrayList<>();
        choices.forEach(choice -> choice.getMessage().getToolCalls().stream()
                .filter(functionCall -> functionCall.getFunction().get("name").equals(functionName))
                .forEach(toolCall -> {
                    String args = toolCall.getFunction().get("arguments");
                    T t = JSON.parseObject(args, clazz);
                    result.add(t);
                }));
        result.forEach(consumer);
        return !result.isEmpty();
    }


    public Usage getUsage() {
        return usage;
    }

    public void setUsage(Usage usage) {
        this.usage = usage;
    }

    public String getPromptLogprobs() {
        return promptLogprobs;
    }

    public void setPromptLogprobs(String promptLogprobs) {
        this.promptLogprobs = promptLogprobs;
    }

}
