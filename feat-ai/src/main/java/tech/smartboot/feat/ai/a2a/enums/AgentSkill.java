/*
 *  Copyright (C) [2024] smartboot [zhengjunweimail@163.com]
 *
 *  企业用户未经smartboot组织特别许可，需遵循AGPL-3.0开源协议合理合法使用本项目。
 *
 *   Enterprise users are required to use this project reasonably
 *   and legally in accordance with the AGPL-3.0 open source agreement
 *  without special permission from the smartboot organization.
 */

package tech.smartboot.feat.ai.a2a.enums;

/**
 * A2A 智能体技能枚举
 *
 * <p>定义了A2A协议中智能体可以具备的技能类型，用于在AgentCard中声明智能体的能力。</p>
 *
 * @author 三刀 zhengjunweimail@163.com
 * @version v1.0 3/12/26
 */
public enum AgentSkill {
    /**
     * 文本生成技能
     */
    TEXT_GENERATION("text_generation", "Text Generation", "Generate text content based on prompts"),

    /**
     * 代码生成技能
     */
    CODE_GENERATION("code_generation", "Code Generation", "Generate code in various programming languages"),

    /**
     * 数据分析技能
     */
    DATA_ANALYSIS("data_analysis", "Data Analysis", "Analyze and process data"),

    /**
     * 图像生成技能
     */
    IMAGE_GENERATION("image_generation", "Image Generation", "Generate images from text descriptions"),

    /**
     * 语音识别技能
     */
    SPEECH_RECOGNITION("speech_recognition", "Speech Recognition", "Convert speech to text"),

    /**
     * 语音合成技能
     */
    SPEECH_SYNTHESIS("speech_synthesis", "Speech Synthesis", "Convert text to speech"),

    /**
     * 翻译技能
     */
    TRANSLATION("translation", "Translation", "Translate content between languages"),

    /**
     * 问答技能
     */
    QUESTION_ANSWERING("question_answering", "Question Answering", "Answer questions based on context"),

    /**
     * 摘要技能
     */
    SUMMARIZATION("summarization", "Summarization", "Summarize long content"),

    /**
     * 分类技能
     */
    CLASSIFICATION("classification", "Classification", "Classify content into categories"),

    /**
     * 工具使用技能
     */
    TOOL_USE("tool_use", "Tool Use", "Use external tools and APIs"),

    /**
     * 多轮对话技能
     */
    MULTI_TURN_CONVERSATION("multi_turn_conversation", "Multi-turn Conversation", "Engage in multi-turn conversations");

    private final String id;
    private final String name;
    private final String description;

    AgentSkill(String id, String name, String description) {
        this.id = id;
        this.name = name;
        this.description = description;
    }

    public String getId() {
        return id;
    }

    public String getName() {
        return name;
    }

    public String getDescription() {
        return description;
    }

    /**
     * 根据ID查找技能
     *
     * @param id 技能ID
     * @return 对应的AgentSkill枚举，如果未找到则返回null
     */
    public static AgentSkill fromId(String id) {
        for (AgentSkill skill : values()) {
            if (skill.id.equals(id)) {
                return skill;
            }
        }
        return null;
    }
}
