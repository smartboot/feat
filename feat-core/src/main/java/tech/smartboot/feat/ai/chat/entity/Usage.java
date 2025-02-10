package tech.smartboot.feat.ai.chat.entity;

import com.alibaba.fastjson2.annotation.JSONField;

public class Usage {
    @JSONField(name = "prompt_tokens")
    private int promptTokens;
    @JSONField(name = "total_tokens")
    private int totalTokens;
    @JSONField(name = "completion_tokens")
    private int completionTokens;

    public int getPromptTokens() {
        return promptTokens;
    }

    public void setPromptTokens(int promptTokens) {
        this.promptTokens = promptTokens;
    }

    public int getTotalTokens() {
        return totalTokens;
    }

    public void setTotalTokens(int totalTokens) {
        this.totalTokens = totalTokens;
    }

    public int getCompletionTokens() {
        return completionTokens;
    }

    public void setCompletionTokens(int completionTokens) {
        this.completionTokens = completionTokens;
    }

    @Override
    public String toString() {
        return "Usage{" +
                "promptTokens=" + promptTokens +
                ", totalTokens=" + totalTokens +
                ", completionTokens=" + completionTokens +
                '}';
    }
}
