/*
 *  Copyright (C) [2024] smartboot [zhengjunweimail@163.com]
 *
 *  企业用户未经smartboot组织特别许可，需遵循AGPL-3.0开源协议合理合法使用本项目。
 *
 *   Enterprise users are required to use this project reasonably
 *   and legally in accordance with the AGPL-3.0 open source agreement
 *  without special permission from the smartboot organization.
 */

package tech.smartboot.feat.ai.agent.tool;

import com.alibaba.fastjson2.JSONObject;
import java.util.Scanner;

/**
 * 终端交互工具，用于Agent执行过程中需要用户输入补充信息的场景
 *
 * @author 三刀
 * @version v1.0 9/30/25
 */
public class ShellInputTool implements ToolExecutor {
    private static final String PARAM_PROMPT = "prompt";
    private static final String PARAM_DEFAULT_VALUE = "default";

    @Override
    public String execute(JSONObject parameters) {
        String prompt = parameters.getString(PARAM_PROMPT);
        String defaultValue = parameters.getString(PARAM_DEFAULT_VALUE);
        
        if (prompt == null || prompt.isEmpty()) {
            prompt = "请输入信息:";
        }
        
        System.out.print(prompt);
        if (defaultValue != null && !defaultValue.isEmpty()) {
            System.out.print(" (默认值: " + defaultValue + ")");
        }
        System.out.print(" ");
        
        Scanner scanner = new Scanner(System.in);
        String input = scanner.nextLine();
        
        // 如果用户没有输入任何内容且有默认值，则返回默认值
        if (input.isEmpty() && defaultValue != null) {
            return defaultValue;
        }
        
        return input;
    }

    @Override
    public String getName() {
        return "shell_input";
    }

    @Override
    public String getDescription() {
        return "终端交互工具，用于在Agent执行过程中请求用户输入补充信息。当AI需要获取用户确认、选择或提供特定信息时使用此工具。";
    }

    @Override
    public String getParametersSchema() {
        return "{\n" +
                "  \"type\": \"object\",\n" +
                "  \"properties\": {\n" +
                "    \"" + PARAM_PROMPT + "\": {\n" +
                "      \"type\": \"string\",\n" +
                "      \"description\": \"提示用户输入的信息，应该清晰地说明需要用户输入的内容\"\n" +
                "    },\n" +
                "    \"" + PARAM_DEFAULT_VALUE + "\": {\n" +
                "      \"type\": \"string\",\n" +
                "      \"description\": \"可选的默认值，当用户未输入直接回车时使用此值\"\n" +
                "    }\n" +
                "  },\n" +
                "  \"required\": [\"" + PARAM_PROMPT + "\"]\n" +
                "}";
    }
}