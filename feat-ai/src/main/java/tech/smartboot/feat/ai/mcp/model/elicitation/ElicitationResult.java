/*
 *  Copyright (C) [2024] smartboot [zhengjunweimail@163.com]
 *
 *  企业用户未经smartboot组织特别许可，需遵循Apache-2.0开源协议合理合法使用本项目。
 *
 *   Enterprise users are required to use this project reasonably
 *   and legally in accordance with the Apache-2.0 open source agreement
 *  without special permission from the smartboot organization.
 */

package tech.smartboot.feat.ai.mcp.model.elicitation;

import com.alibaba.fastjson2.JSONObject;

/**
 * elicitation/create 的三动作结果。
 * <p>
 * url 模式下 action=accept 只代表用户同意发起交互，不代表已完成；完成情况通过
 * notifications/elicitation/complete 通知回传，且不会携带 content。
 *
 * @author 三刀 zhengjunweimail@163.com
 * @version v1.0
 */
public class ElicitationResult {
    public static final String ACTION_ACCEPT = "accept";
    public static final String ACTION_DECLINE = "decline";
    public static final String ACTION_CANCEL = "cancel";

    private String action;
    /**
     * 仅 form 模式的 accept 携带，需符合 requestedSchema。
     */
    private JSONObject content;

    private ElicitationResult(String action, JSONObject content) {
        this.action = action;
        this.content = content;
    }

    public static ElicitationResult accept(JSONObject content) {
        return new ElicitationResult(ACTION_ACCEPT, content);
    }

    public static ElicitationResult accept() {
        return new ElicitationResult(ACTION_ACCEPT, null);
    }

    public static ElicitationResult decline() {
        return new ElicitationResult(ACTION_DECLINE, null);
    }

    public static ElicitationResult cancel() {
        return new ElicitationResult(ACTION_CANCEL, null);
    }

    public String getAction() {
        return action;
    }

    public void setAction(String action) {
        this.action = action;
    }

    public JSONObject getContent() {
        return content;
    }

    public void setContent(JSONObject content) {
        this.content = content;
    }
}
