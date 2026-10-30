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
import tech.smartboot.feat.core.common.FeatUtils;

/**
 * elicitation/create 的请求参数。
 * <p>
 * 两种模式：form 模式在带内收集结构化数据；url 模式只把用户引导到外部页面，
 * 敏感信息（密码、API Key、支付凭证）必须走 url 模式，不得放在 form 里。
 *
 * @author 三刀 zhengjunweimail@163.com
 * @version v1.0
 */
public class ElicitationRequest {
    public static final String MODE_FORM = "form";
    public static final String MODE_URL = "url";

    /**
     * 交互模式，缺省按 form 处理。
     */
    private String mode = MODE_FORM;
    /**
     * 向用户解释为什么需要这次交互。
     */
    private String message;
    /**
     * form 模式的结构化校验 schema。
     */
    private JSONObject requestedSchema;
    /**
     * url 模式：用户应访问的地址。
     */
    private String url;
    /**
     * url 模式：本次交互的唯一标识，用于匹配完成通知。
     */
    private String elicitationId;

    private ElicitationRequest() {
    }

    public static ElicitationRequest form(String message, ElicitationSchema schema) {
        ElicitationRequest request = new ElicitationRequest();
        request.mode = MODE_FORM;
        request.message = message;
        request.requestedSchema = schema == null ? null : schema.toJSON();
        return request;
    }

    public static ElicitationRequest url(String message, String url, String elicitationId) {
        if (FeatUtils.isBlank(url)) {
            throw new IllegalArgumentException("url is required for url mode elicitation");
        }
        ElicitationRequest request = new ElicitationRequest();
        request.mode = MODE_URL;
        request.message = message;
        request.url = url;
        request.elicitationId = elicitationId;
        return request;
    }

    public String getMode() {
        return mode;
    }

    public void setMode(String mode) {
        if (!MODE_FORM.equals(mode) && !MODE_URL.equals(mode)) {
            throw new IllegalArgumentException("elicitation mode must be form or url: " + mode);
        }
        this.mode = mode;
    }

    public String getMessage() {
        return message;
    }

    public void setMessage(String message) {
        this.message = message;
    }

    public JSONObject getRequestedSchema() {
        return requestedSchema;
    }

    public void setRequestedSchema(JSONObject requestedSchema) {
        this.requestedSchema = requestedSchema;
    }

    public String getUrl() {
        return url;
    }

    public void setUrl(String url) {
        this.url = url;
    }

    public String getElicitationId() {
        return elicitationId;
    }

    public void setElicitationId(String elicitationId) {
        this.elicitationId = elicitationId;
    }
}
