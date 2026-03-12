/*
 *  Copyright (C) [2024] smartboot [zhengjunweimail@163.com]
 *
 *  企业用户未经smartboot组织特别许可，需遵循AGPL-3.0开源协议合理合法使用本项目。
 *
 *   Enterprise users are required to use this project reasonably
 *   and legally in accordance with the AGPL-3.0 open source agreement
 *  without special permission from the smartboot organization.
 */

package tech.smartboot.feat.ai.a2a.model;

import com.alibaba.fastjson2.JSONObject;

import java.util.List;

/**
 * A2A 智能体能力描述类
 *
 * <p>描述智能体支持的能力，如流式处理、推送通知等。</p>
 *
 * @author 三刀 zhengjunweimail@163.com
 * @version v1.0 3/12/26
 */
public class AgentCapability {
    /**
     * 是否支持流式响应
     */
    private boolean streaming;

    /**
     * 是否支持推送通知
     */
    private boolean pushNotifications;

    /**
     * 是否支持状态更新通知
     */
    private boolean stateTransitioning;

    /**
     * 其他扩展能力
     */
    private JSONObject customCapabilities;

    public boolean isStreaming() {
        return streaming;
    }

    public void setStreaming(boolean streaming) {
        this.streaming = streaming;
    }

    public boolean isPushNotifications() {
        return pushNotifications;
    }

    public void setPushNotifications(boolean pushNotifications) {
        this.pushNotifications = pushNotifications;
    }

    public boolean isStateTransitioning() {
        return stateTransitioning;
    }

    public void setStateTransitioning(boolean stateTransitioning) {
        this.stateTransitioning = stateTransitioning;
    }

    public JSONObject getCustomCapabilities() {
        return customCapabilities;
    }

    public void setCustomCapabilities(JSONObject customCapabilities) {
        this.customCapabilities = customCapabilities;
    }
}
