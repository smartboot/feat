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

import tech.smartboot.feat.core.common.exception.FeatException;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.function.Function;

/**
 * @author 三刀 zhengjunweimail@163.com
 * @version v1.0.0
 */
public class Prompt {
    public static final String CONTENT_PARAM_NAME = "content";
    private final List<Function<Map<String, String>, String>> promptBuilder;

    public Prompt(String prompt) {
        this.promptBuilder = parse(prompt);
    }

    private boolean noneParam = true;

    private List<Function<Map<String, String>, String>> parse(String prompt) {
        List<Function<Map<String, String>, String>> promptBuilder = new ArrayList<>();
        int offset = 0;
        while (offset != prompt.length()) {
            int leftIndex = prompt.indexOf("{{", offset);
            if (leftIndex == -1) {
                String part = prompt.substring(offset);
                promptBuilder.add(stringStringMap -> part);
                break;
            }
            int rightIndex = prompt.indexOf("}}", leftIndex);
            if (rightIndex == -1) {
                //非法异常
                throw new FeatException("prompt: " + prompt + " is invalid");
            }
            noneParam = false;
            String param = prompt.substring(leftIndex + 2, rightIndex);
            promptBuilder.add(stringStringMap -> stringStringMap.getOrDefault(param, ""));
            offset = rightIndex + 2;
        }
        if (noneParam) {
            promptBuilder.add(stringStringMap -> "\r\nUser:" + stringStringMap.getOrDefault(CONTENT_PARAM_NAME, ""));
        }
        return promptBuilder;
    }

    public boolean isNoneParam() {
        return noneParam;
    }

    /**
     * 提示词
     *
     * @param params 提示词参数
     * @return 提示词
     */
    public final String prompt(Map<String, String> params) {
        StringBuilder prompt = new StringBuilder();
        for (Function<Map<String, String>, String> function : promptBuilder) {
            prompt.append(function.apply(params));
        }
        return prompt.toString();
    }
}