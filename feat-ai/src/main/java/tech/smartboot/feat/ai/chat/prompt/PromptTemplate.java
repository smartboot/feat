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

import tech.smartboot.feat.ai.ModelMeta;
import tech.smartboot.feat.core.common.FeatUtils;
import tech.smartboot.feat.core.common.exception.FeatException;

import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * @author 三刀 zhengjunweimail@163.com
 * @version v1.0.0
 */
public class PromptTemplate {
    public static Prompt MAVEN_PROJECT_MERMAID = new Prompt() {
        private final Set<String> prompts = new HashSet<>(Arrays.asList("file_list"));

        @Override
        public String prompt(Map<String, String> params) {
            String prompt = getPromptTpl("maven_project_mermaid.tpl");
            return mergePrompt(prompt, params(), params);
        }

        @Override
        public Set<String> params() {
            return prompts;
        }

        @Override
        public List<String> suggestedModels() {
            return Arrays.asList(ModelMeta.GITEE_AI_Qwen2_5_32B_Instruct.getModel());
        }
    };

    /**
     * 微信公众号文章编辑器
     */
    public static Prompt WECHAT_EDITOR = new Prompt() {
        private final Set<String> prompts = new HashSet<>(Arrays.asList("topic", "reference"));

        @Override
        public String prompt(Map<String, String> params) {
            String prompt = getPromptTpl("wechat_editor.tpl");
            return mergePrompt(prompt, params(), params);
        }

        @Override
        public Set<String> params() {
            return prompts;
        }

        @Override
        public List<String> suggestedModels() {
            return Arrays.asList(ModelMeta.GITEE_AI_Qwen2_5_32B_Instruct.getModel());
        }
    };
    /**
     * 项目代码生成器
     */
    public static final Prompt PROJECT_CODER = new Prompt() {
        private final Set<String> prompts = new HashSet<>(Arrays.asList("input", "reference"));

        @Override
        public String prompt(Map<String, String> params) {
            String prompt = getPromptTpl("project_coder.tpl");
            return mergePrompt(prompt, params(), params);
        }

        @Override
        public Set<String> params() {
            return prompts;
        }

        @Override
        public List<String> suggestedModels() {
            return Arrays.asList(ModelMeta.GITEE_AI_Qwen2_5_32B_Instruct.getModel());
        }
    };

    /**
     * 项目文档编辑器
     */
    public static Prompt PROJECT_DOCUMENT_EDITOR = new Prompt() {
        public static final String FIELD_INPUT = "input";
        public static final String FIELD_REF_SOURCE = "ref_source";
        public static final String FIELD_REF_DOC = "ref_doc";
        private final Set<String> prompts = new HashSet<>(Arrays.asList(FIELD_INPUT, FIELD_REF_SOURCE, FIELD_REF_DOC));

        @Override
        public String prompt(Map<String, String> params) {
            String prompt = getPromptTpl("project_document_editor.tpl");
            return mergePrompt(prompt, params(), params);
        }

        @Override
        public Set<String> params() {
            return prompts;
        }

        @Override
        public List<String> suggestedModels() {
            return Arrays.asList(ModelMeta.GITEE_AI_Qwen2_5_32B_Instruct.getModel());
        }
    };

    private static String mergePrompt(String prompt, Set<String> params, Map<String, String> data) {
        for (String param : params) {
            String value = data.get(param);
            if (value == null) {
                throw new FeatException("param: " + param + " not found");
            }
            prompt = prompt.replace("{{" + param + "}}", data.get(param));
        }
        return prompt;
    }

    private static String getPromptTpl(String fileName) {
        String prompt = FeatUtils.getResourceAsString("feat-prompts/" + fileName);
        if (prompt == null) {
            throw new FeatException("prompt: " + fileName + " not found");
        }
        return prompt;
    }
}
