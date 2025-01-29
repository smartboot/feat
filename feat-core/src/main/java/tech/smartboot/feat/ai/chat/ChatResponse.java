package tech.smartboot.feat.ai.chat;

import com.alibaba.fastjson2.JSON;
import com.alibaba.fastjson2.annotation.JSONField;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Consumer;

public class ChatResponse {
    private String id;
    private String object;
    private long created;
    private String model;
    private List<Choice> choices;
    private Usage usage;
    @JSONField(name = "prompt_logprobs")
    private String promptLogprobs;

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public String getObject() {
        return object;
    }

    public void setObject(String object) {
        this.object = object;
    }

    public long getCreated() {
        return created;
    }

    public void setCreated(long created) {
        this.created = created;
    }

    public String getModel() {
        return model;
    }

    public void setModel(String model) {
        this.model = model;
    }

    public List<Choice> getChoices() {
        return choices;
    }

    public void setChoices(List<Choice> choices) {
        this.choices = choices;
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
}
