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

import java.util.List;

/**
 * @author 三刀(zhengjunweimail@163.com)
 * @version v1.0.0
 */
public class ChatStreamResponse extends ChatResponse {

    /**
     * 是一个包含一个或多个聊天响应的列表。如果请求的参数 n 大于 1时(请求模型生成多个答复)，列表的元素将是多个。
     * 一般情况下 choices 只包含一个元素
     */
    private List<StreamChoice> choices;

    public StreamChoice getChoice() {
        return choices.get(0);
    }

    public List<StreamChoice> getChoices() {
        return choices;
    }

    public void setChoices(List<StreamChoice> choices) {
        this.choices = choices;
    }

}
