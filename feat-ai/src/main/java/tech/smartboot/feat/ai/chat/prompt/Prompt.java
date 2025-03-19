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

import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * @author 三刀(zhengjunweimail@163.com)
 * @version v1.0.0
 */
public interface Prompt {
    /**
     * 建议使用的模型, 可多个
     *
     * @return 模型
     */
    default List<String> suggestedModels() {
        return Collections.emptyList();
    }

    /**
     * 角色
     * @return 角色
     */
    default String role() {
        return "";
    }

    /**
     * 提示词
     * @param params 提示词参数
     * @return 提示词
     */
    String prompt(Map<String, String> params);

    /**
     * 提示词参数
     * @return 参数
     */
    Set<String> params();
}
