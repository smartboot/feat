package tech.smartboot.feat.ai.prompt;

import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Set;

public interface Prompt {
    /**
     * 建议使用的模型, 可多个
     *
     * @return 模型
     */
    public default List<String> suggestedModels() {
        return Collections.emptyList();
    }

    /**
     * 角色
     * @return 角色
     */
    public default String role() {
        return "";
    }

    /**
     * 提示词
     * @param params 提示词参数
     * @return 提示词
     */
    public String prompt(Map<String, String> params);

    /**
     * 提示词参数
     * @return 参数
     */
    public Set<String> params();
}
