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
import tech.smartboot.feat.core.common.exception.FeatException;

import java.util.List;
import java.util.Map;

/**
 * @author 三刀 zhengjunweimail@163.com
 * @version v1.0.0
 */
public class Prompt {
    private final String promptFile;

    private final List<String> paramNames;

    public Prompt(String promptFile, List<String> params) {
        this.promptFile = promptFile;
        this.paramNames = params;
    }

    /**
     * 提示词
     *
     * @param params 提示词参数
     * @return 提示词
     */
    public final String prompt(Map<String, String> params) {
        String prompt = FeatUtils.getResourceAsString("feat-prompts/" + promptFile);
        if (prompt == null) {
            throw new FeatException("prompt: " + promptFile + " not found");
        }
        for (String param : paramNames) {
            String value = params.get(param);
            if (value == null) {
                throw new FeatException("param: " + param + " not found");
            }
            prompt = prompt.replace("{{" + param + "}}", params.get(param));
        }
        return prompt;
    }

}
