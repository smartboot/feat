package tech.smartboot.feat.ai.prompt;

import tech.smartboot.feat.ai.ModelMeta;
import tech.smartboot.feat.core.common.FeatUtils;
import tech.smartboot.feat.core.common.exception.FeatException;

import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * Prompt模板
 * @author 三刀
 * @version V1.0
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
