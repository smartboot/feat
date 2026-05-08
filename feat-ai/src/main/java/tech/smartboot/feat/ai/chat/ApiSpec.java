/*
 *  Copyright (C) [2024] smartboot [zhengjunweimail@163.com]
 *
 *  企业用户未经smartboot组织特别许可，需遵循Apache-2.0开源协议合理合法使用本项目。
 *
 *   Enterprise users are required to use this project reasonably
 *   and legally in accordance with the Apache-2.0 open source agreement
 *  without special permission from the smartboot organization.
 */

package tech.smartboot.feat.ai.chat;

/**
 * API 规范枚举，定义支持的聊天模型 API 规范类型
 *
 * @author 三刀 zhengjunweimail@163.com
 * @version v1.0.0
 */
public enum ApiSpec {
    /**
     * OpenAI API 规范（默认）
     * 兼容 OpenAI、Qwen、DeepSeek 等遵循 OpenAI 规范的模型
     */
    OPENAI,

    /**
     * Anthropic API 规范
     * 兼容 Anthropic Claude、以及阿里云 DashScope 等支持 Anthropic 规范的模型
     */
    ANTHROPIC
}
