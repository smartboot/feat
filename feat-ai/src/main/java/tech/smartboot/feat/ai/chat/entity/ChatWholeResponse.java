/*
 *  Copyright (C) [2024] smartboot [zhengjunweimail@163.com]
 *
 *  企业用户未经smartboot组织特别许可，需遵循AGPL-3.0开源协议合理合法使用本项目。
 *
 *   Enterprise users are required to use this project reasonably
 *   and legally in accordance with the AGPL-3.0 open source agreement
 *  without special permission from the smartboot organization.
 */

package tech.smartboot.feat.ai.chat.entity;

import com.alibaba.fastjson2.JSON;
import com.alibaba.fastjson2.annotation.JSONField;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Consumer;

/**
 * 完整聊天响应类，继承自ChatResponse，包含完整的聊天响应信息
 *
 * @author 三刀 zhengjunweimail@163.com
 * @version v1.0.0
 */
public class ChatWholeResponse extends ChatResponse {

    /**
     * 是一个包含一个或多个聊天响应的列表。如果请求的参数 n 大于 1时(请求模型生成多个答复)，列表的元素将是多个。
     * 一般情况下 choices 只包含一个元素
     */
    private List<WholeChoice> choices;

    /**
     * 使用情况统计信息，包括提示词token数、完成内容token数等
     */
    private Usage usage;

    /**
     * 提示词对数概率信息
     */
    @JSONField(name = "prompt_logprobs")
    private String promptLogprobs;

    /**
     * 获取第一个选择项（通常也是唯一一个）
     *
     * @return 第一个选择项
     */
    public WholeChoice getChoice() {
        return choices.get(0);
    }

    /**
     * 获取所有选择项列表
     *
     * @return 选择项列表
     */
    public List<WholeChoice> getChoices() {
        return choices;
    }

    /**
     * 设置选择项列表
     *
     * @param choices 选择项列表
     */
    public void setChoices(List<WholeChoice> choices) {
        this.choices = choices;
    }

    /**
     * 检查并处理匹配的工具调用
     *
     * @param <T>          泛型参数，表示工具调用参数的类型
     * @param functionName 工具函数名称
     * @param clazz        工具调用参数类型的Class对象
     * @param consumer     处理匹配到的工具调用的消费者函数
     * @return 如果找到匹配的工具调用则返回true，否则返回false
     */
    public <T> boolean ifMatchTool(String functionName, Class<T> clazz, Consumer<T> consumer) {
        List<T> result = new ArrayList<>();
        choices.forEach(choice -> choice.getMessage().getToolCalls().stream()
                .filter(functionCall -> functionCall.getFunction().get("name").equals(functionName))
                .forEach(toolCall -> {
                    String args = toolCall.getFunction().get("arguments");
                    T t = JSON.parseObject(args, clazz);
                    result.add(t);
                }));
        result.forEach(consumer);
        return !result.isEmpty();
    }

    /**
     * 获取使用情况统计信息
     *
     * @return 使用情况统计信息
     */
    public Usage getUsage() {
        return usage;
    }

    /**
     * 设置使用情况统计信息
     *
     * @param usage 使用情况统计信息
     */
    public void setUsage(Usage usage) {
        this.usage = usage;
    }

    /**
     * 获取提示词对数概率信息
     *
     * @return 提示词对数概率信息字符串
     */
    public String getPromptLogprobs() {
        return promptLogprobs;
    }

    /**
     * 设置提示词对数概率信息
     *
     * @param promptLogprobs 提示词对数概率信息
     */
    public void setPromptLogprobs(String promptLogprobs) {
        this.promptLogprobs = promptLogprobs;
    }

}