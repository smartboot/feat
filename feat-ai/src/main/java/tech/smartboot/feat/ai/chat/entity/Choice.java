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
 * 选择项类，表示聊天响应中的一个选择项
 *
 * @author 三刀 zhengjunweimail@163.com
 * @version v1.0.0
 */
public class Choice {
    /**
     * 选择项索引
     */
    private int index;

    /**
     * 对数概率信息
     */
    private String logprobs;

    /**
     * 完成原因，表示模型为什么停止生成文本
     */
    @JSONField(name = "finish_reason")
    private String finishReason;

    /**
     * 停止原因
     */
    @JSONField(name = "stop_reason")
    private String stopReason;

    /**
     * 获取选择项索引
     *
     * @return 选择项索引
     */
    public int getIndex() {
        return index;
    }

    /**
     * 设置选择项索引
     *
     * @param index 选择项索引
     */
    public void setIndex(int index) {
        this.index = index;
    }

    /**
     * 获取对数概率信息
     *
     * @return 对数概率信息
     */
    public String getLogprobs() {
        return logprobs;
    }

    /**
     * 设置对数概率信息
     *
     * @param logprobs 对数概率信息
     */
    public void setLogprobs(String logprobs) {
        this.logprobs = logprobs;
    }

    /**
     * 获取完成原因
     *
     * @return 完成原因
     */
    public String getFinishReason() {
        return finishReason;
    }

    /**
     * 设置完成原因
     *
     * @param finishReason 完成原因
     */
    public void setFinishReason(String finishReason) {
        this.finishReason = finishReason;
    }

    /**
     * 获取停止原因
     *
     * @return 停止原因
     */
    public String getStopReason() {
        return stopReason;
    }

    /**
     * 设置停止原因
     *
     * @param stopReason 停止原因
     */
    public void setStopReason(String stopReason) {
        this.stopReason = stopReason;
    }
}