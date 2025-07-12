/*
 *  Copyright (C) [2024] smartboot [zhengjunweimail@163.com]
 *
 *  企业用户未经smartboot组织特别许可，需遵循AGPL-3.0开源协议合理合法使用本项目。
 *
 *   Enterprise users are required to use this project reasonably
 *   and legally in accordance with the AGPL-3.0 open source agreement
 *  without special permission from the smartboot organization.
 */

package tech.smartboot.feat.cloud.mcp.server.model;

import tech.smartboot.feat.cloud.mcp.model.Prompt;

import java.util.function.Function;

/**
 * @author 三刀
 * @version v1.0 7/11/25
 */
public class ServerPrompt extends Prompt {
    private Function<PromptContext, PromptResult> action;

    public static ServerPrompt of(String name) {
        return new ServerPrompt(name);
    }

    protected ServerPrompt(String name) {
        super(name);
    }

    @Override
    public ServerPrompt title(String title) {
        super.title(title);
        return this;
    }

    @Override
    public ServerPrompt description(String description) {
        super.description(description);
        return this;
    }

    @Override
    public ServerPrompt arguments(Argument... arguments) {
        super.arguments(arguments);
        return this;
    }

    public ServerPrompt doAction(Function<PromptContext, PromptResult> action) {
        this.action = action;
        return this;
    }

    public Function<PromptContext, PromptResult> getAction() {
        return action;
    }
}
