/*
 *  Copyright (C) [2024] smartboot [zhengjunweimail@163.com]
 *
 *  企业用户未经smartboot组织特别许可，需遵循AGPL-3.0开源协议合理合法使用本项目。
 *
 *   Enterprise users are required to use this project reasonably
 *   and legally in accordance with the AGPL-3.0 open source agreement
 *  without special permission from the smartboot organization.
 */

package tech.smartboot.feat.ai.chat.prompt;

import tech.smartboot.feat.core.common.FeatUtils;

/**
 * 提示词模板类，预定义常用的提示词模板
 *
 * @author 三刀 zhengjunweimail@163.com
 * @version v1.0.0
 */
public class PromptTemplate {

    /**
     * Maven项目Mermaid图表提示词模板
     */
    public static Prompt MAVEN_PROJECT_MERMAID = new Prompt(loadPrompt("maven_project_mermaid.tpl"));

    /**
     * 微信公众号文章编辑器提示词模板
     */
    public static Prompt WECHAT_EDITOR = new Prompt(loadPrompt("wechat_editor.tpl"));

    /**
     * 项目代码生成器提示词模板
     */
    public static final Prompt PROJECT_CODER = new Prompt(loadPrompt("project_coder.tpl"));

    /**
     * 项目文档编辑器提示词模板
     */
    public static Prompt PROJECT_DOCUMENT_EDITOR = new Prompt(loadPrompt("project_document_editor.tpl"));

    /**
     * 加载提示词模板文件
     *
     * @param promptFile 提示词模板文件名
     * @return 提示词模板内容
     */
    public static String loadPrompt(String promptFile) {
        return FeatUtils.getResourceAsString("feat-prompts/" + promptFile);
    }
}