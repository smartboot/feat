/*
 *  Copyright (C) [2024] smartboot [zhengjunweimail@163.com]
 *
 *  企业用户未经smartboot组织特别许可，需遵循AGPL-3.0开源协议合理合法使用本项目。
 *
 *   Enterprise users are required to use this project reasonably
 *   and legally in accordance with the AGPL-3.0 open source agreement
 *  without special permission from the smartboot organization.
 */

package tech.smartboot.feat.cloud.mcp.model;

import java.util.ArrayList;
import java.util.List;

/**
 * @author 三刀
 * @version v1.0 7/11/25
 */
public class GetPromptResult {
    private String description;
    private final List<PromptMessage<?>> messages = new ArrayList<>();

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public List<PromptMessage<?>> getMessages() {
        return messages;
    }

    public void addMessage(PromptMessage<?> message) {
        this.messages.add(message);
    }
}
