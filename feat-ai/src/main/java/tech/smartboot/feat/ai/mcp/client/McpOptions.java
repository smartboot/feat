/*
 *  Copyright (C) [2024] smartboot [zhengjunweimail@163.com]
 *
 *  企业用户未经smartboot组织特别许可，需遵循AGPL-3.0开源协议合理合法使用本项目。
 *
 *   Enterprise users are required to use this project reasonably
 *   and legally in accordance with the AGPL-3.0 open source agreement
 *  without special permission from the smartboot organization.
 */

package tech.smartboot.feat.ai.mcp.client;

import com.alibaba.fastjson2.JSONObject;
import tech.smartboot.feat.Feat;
import tech.smartboot.feat.ai.mcp.model.Implementation;
import tech.smartboot.feat.ai.mcp.model.Roots;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Consumer;

/**
 * @author 三刀 zhengjunweimail@163.com
 * @version v1.0 7/8/25
 */
public class McpOptions {
    private String url;
    private final Implementation implementation = Implementation.of("feat-mcp-client", "Feat MCP", Feat.VERSION);
    private boolean roots;
    private boolean sampling;
    private boolean elicitation;
    private JSONObject experimental;
    private List<Roots> rootsList;
    private Map<String, String> headers = Collections.emptyMap();
    private Consumer<String> notificationHandler = (method) -> {
    };
    private boolean debug = false;

    McpOptions() {
    }

    public String getUrl() {
        return url;
    }

    public McpOptions url(String url) {
        this.url = url;
        return this;
    }

    public Implementation getImplementation() {
        return implementation;
    }

    public void implementation(String name, String title, String version) {
        this.implementation.setName(name);
        this.implementation.setTitle(title);
        this.implementation.setVersion(version);
    }

    public McpOptions rootsEnable() {
        this.roots = true;
        this.rootsList = new ArrayList<>();
        return this;
    }

    public McpOptions samplingEnable() {
        this.sampling = true;
        return this;
    }

    public McpOptions elicitationEnable() {
        this.elicitation = true;
        return this;
    }

    boolean isRoots() {
        return roots;
    }

    boolean isSampling() {
        return sampling;
    }

    boolean isElicitation() {
        return elicitation;
    }

    JSONObject getExperimental() {
        return experimental;
    }

    List<Roots> getRootsList() {
        return rootsList;
    }

    Consumer<String> getNotificationHandler() {
        return notificationHandler;
    }

    void setNotificationHandler(Consumer<String> notificationHandler) {
        this.notificationHandler = notificationHandler;
    }

    public McpOptions header(Consumer<Map<String, String>> header) {
        if (header == null) {
            throw new NullPointerException();
        }
        if (this.headers.equals(Collections.emptyMap())) {
            this.headers = new HashMap<>();
        }
        header.accept(this.headers);
        return this;
    }

    Map<String, String> getHeaders() {
        return headers;
    }

    public McpOptions debug(boolean debug) {
        this.debug = debug;
        return this;
    }

    public boolean debug() {
        return debug;
    }
}
