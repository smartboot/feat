/*
 *  Copyright (C) [2024] smartboot [zhengjunweimail@163.com]
 *
 *  企业用户未经smartboot组织特别许可，需遵循AGPL-3.0开源协议合理合法使用本项目。
 *
 *   Enterprise users are required to use this project reasonably
 *   and legally in accordance with the AGPL-3.0 open source agreement
 *  without special permission from the smartboot organization.
 */

package tech.smartboot.feat.ai.chat.entity;

import com.alibaba.fastjson2.annotation.JSONField;

/**
 * @author 三刀 zhengjunweimail@163.com
 * @version v1.0.0
 */
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
