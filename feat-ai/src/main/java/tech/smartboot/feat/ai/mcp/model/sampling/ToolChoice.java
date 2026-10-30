/*
 *  Copyright (C) [2024] smartboot [zhengjunweimail@163.com]
 *
 *  企业用户未经smartboot组织特别许可，需遵循Apache-2.0开源协议合理合法使用本项目。
 *
 *   Enterprise users are required to use this project reasonably
 *   and legally in accordance with the Apache-2.0 open source agreement
 *  without special permission from the smartboot organization.
 */

package tech.smartboot.feat.ai.mcp.model.sampling;

/**
 * sampling 请求中的工具选择策略。
 *
 * @author 三刀 zhengjunweimail@163.com
 * @version v1.0
 */
public class ToolChoice {
    public static final String MODE_AUTO = "auto";
    public static final String MODE_REQUIRED = "required";
    public static final String MODE_NONE = "none";

    private String mode;

    private ToolChoice(String mode) {
        this.mode = mode;
    }

    /**
     * 由模型自行决定是否使用工具，缺省行为。
     */
    public static ToolChoice auto() {
        return new ToolChoice(MODE_AUTO);
    }

    /**
     * 模型在结束前必须至少调用一个工具。
     */
    public static ToolChoice required() {
        return new ToolChoice(MODE_REQUIRED);
    }

    /**
     * 模型不得调用工具，常用于多轮工具循环的最后一轮，强制输出最终结果。
     */
    public static ToolChoice none() {
        return new ToolChoice(MODE_NONE);
    }

    public String getMode() {
        return mode;
    }

    public void setMode(String mode) {
        if (!MODE_AUTO.equals(mode) && !MODE_REQUIRED.equals(mode) && !MODE_NONE.equals(mode)) {
            throw new IllegalArgumentException("toolChoice mode must be auto, required or none: " + mode);
        }
        this.mode = mode;
    }
}
