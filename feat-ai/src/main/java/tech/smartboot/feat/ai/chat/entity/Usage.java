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
 * 使用情况统计类，记录AI模型调用过程中的Token使用情况
 *
 * @author 三刀 zhengjunweimail@163.com
 * @version v1.0.0
 */
public class Usage {
    /**
     * 提示词Token数量
     */
    @JSONField(name = "prompt_tokens")
    private int promptTokens;

    /**
     * 总Token数量
     */
    @JSONField(name = "total_tokens")
    private int totalTokens;

    /**
     * 完成部分Token数量
     */
    @JSONField(name = "completion_tokens")
    private int completionTokens;

    /**
     * 获取提示词Token数量
     *
     * @return 提示词Token数量
     */
    public int getPromptTokens() {
        return promptTokens;
    }

    /**
     * 设置提示词Token数量
     *
     * @param promptTokens 提示词Token数量
     */
    public void setPromptTokens(int promptTokens) {
        this.promptTokens = promptTokens;
    }

    /**
     * 获取总Token数量
     *
     * @return 总Token数量
     */
    public int getTotalTokens() {
        return totalTokens;
    }

    /**
     * 设置总Token数量
     *
     * @param totalTokens 总Token数量
     */
    public void setTotalTokens(int totalTokens) {
        this.totalTokens = totalTokens;
    }

    /**
     * 获取完成部分Token数量
     *
     * @return 完成部分Token数量
     */
    public int getCompletionTokens() {
        return completionTokens;
    }

    /**
     * 设置完成部分Token数量
     *
     * @param completionTokens 完成部分Token数量
     */
    public void setCompletionTokens(int completionTokens) {
        this.completionTokens = completionTokens;
    }

    /**
     * 返回对象的字符串表示
     *
     * @return 对象的字符串表示
     */
    @Override
    public String toString() {
        return "Usage{" +
                "promptTokens=" + promptTokens +
                ", totalTokens=" + totalTokens +
                ", completionTokens=" + completionTokens +
                '}';
    }
}