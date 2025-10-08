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

import java.util.Arrays;

/**
 * @author 三刀 zhengjunweimail@163.com
 * @version v1.0.0
 */
public class PromptTemplate {
    public static final String PARAM_SYSTEM_PROMPT = "system_prompt";
    public static final String PARAM_TOOLS = "tools";
    public static final String PARAM_QUERY = "query";
    public static final Prompt GENERAL_AGENT_PROMPT = new Prompt("general_agent.tpl", Arrays.asList(PARAM_SYSTEM_PROMPT, PARAM_TOOLS, PARAM_QUERY));

    /**
     * 执行计划规划师提示词模板
     */
    public static final Prompt EXECUTION_PLANNER = new Prompt("execution_planner.tpl", Arrays.asList("user_request", "agents"));

    public static Prompt MAVEN_PROJECT_MERMAID = new Prompt("maven_project_mermaid.tpl", Arrays.asList("file_list"));

    /**
     * 微信公众号文章编辑器
     */
    public static Prompt WECHAT_EDITOR = new Prompt("wechat_editor.tpl", Arrays.asList("topic", "reference"));
    /**
     * 项目代码生成器
     */
    public static final Prompt PROJECT_CODER = new Prompt("project_coder.tpl", Arrays.asList("input", "reference"));


    /**
     * 项目文档编辑器
     */
    public static final String FIELD_INPUT = "input";
    public static final String FIELD_REF_SOURCE = "ref_source";
    public static final String FIELD_REF_DOC = "ref_doc";
    public static Prompt PROJECT_DOCUMENT_EDITOR = new Prompt("project_document_editor.tpl", Arrays.asList(FIELD_INPUT, FIELD_REF_SOURCE, FIELD_REF_DOC));


}
