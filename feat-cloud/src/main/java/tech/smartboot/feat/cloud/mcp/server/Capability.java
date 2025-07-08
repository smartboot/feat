/*
 *  Copyright (C) [2024] smartboot [zhengjunweimail@163.com]
 *
 *  企业用户未经smartboot组织特别许可，需遵循AGPL-3.0开源协议合理合法使用本项目。
 *
 *   Enterprise users are required to use this project reasonably
 *   and legally in accordance with the AGPL-3.0 open source agreement
 *  without special permission from the smartboot organization.
 */

package tech.smartboot.feat.cloud.mcp.server;

import com.alibaba.fastjson2.annotation.JSONField;

public class Capability {
    /**
     * Support for list change notifications (for prompts, resources, and tools)
     */
    private boolean listChanged;
    /**
     * Support for subscribing to individual items’ changes (resources only)
     */
    private boolean subscribe;

    public boolean isListChanged() {
        return listChanged;
    }

    public void setListChanged(boolean listChanged) {
        this.listChanged = listChanged;
    }

    public boolean isSubscribe() {
        return subscribe;
    }

    public void setSubscribe(boolean subscribe) {
        this.subscribe = subscribe;
    }
}