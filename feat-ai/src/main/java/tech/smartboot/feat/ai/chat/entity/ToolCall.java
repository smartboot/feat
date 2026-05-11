/*
 *  Copyright (C) [2024] smartboot [zhengjunweimail@163.com]
 *
 *  企业用户未经smartboot组织特别许可，需遵循Apache-2.0开源协议合理合法使用本项目。
 *
 *   Enterprise users are required to use this project reasonably
 *   and legally in accordance with the Apache-2.0 open source agreement
 *   without special permission from the smartboot organization.
 */

package tech.smartboot.feat.ai.chat.entity;

/**
 * 工具调用类，表示AI模型对工具的调用信息
 *
 * <h3>设计说明：</h3>
 * <p>
 * ToolCall 是 Feat AI 的通用工具调用结构，独立于任何 Provider 的具体实现。
 * 各 Provider 使用内部类处理特定平台的解析逻辑，最终转换为统一的 ToolCall 格式。
 * </p>
 *
 * <h3>通用字段：</h3>
 * <ul>
 *   <li><b>id</b> - 工具调用唯一标识</li>
 *   <li><b>name</b> - 被调用的函数/工具名称</li>
 *   <li><b>arguments</b> - 函数参数（JSON 字符串格式，通用）</li>
 *   <li><b>index</b> - 调用顺序索引</li>
 *   <li><b>type</b> - 调用类型（通常为 "function"）</li>
 * </ul>
 *
 * @author 三刀 zhengjunweimail@163.com
 * @version v2.0.0
 */
public class ToolCall {
    /**
     * 调用索引
     */
    private int index;

    /**
     * 调用ID
     */
    private String id;

    /**
     * 调用类型
     */
    private String type;

    /**
     * 函数/工具名称
     */
    private String name;

    /**
     * 函数参数（JSON 字符串格式）
     */
    private String arguments;

    /**
     * 获取调用索引
     *
     * @return 调用索引
     */
    public int getIndex() {
        return index;
    }

    /**
     * 设置调用索引
     *
     * @param index 调用索引
     */
    public void setIndex(int index) {
        this.index = index;
    }

    /**
     * 获取调用ID
     *
     * @return 调用ID
     */
    public String getId() {
        return id;
    }

    /**
     * 设置调用ID
     *
     * @param id 调用ID
     */
    public void setId(String id) {
        this.id = id;
    }

    /**
     * 获取调用类型
     *
     * @return 调用类型
     */
    public String getType() {
        return type;
    }

    /**
     * 设置调用类型
     *
     * @param type 调用类型
     */
    public void setType(String type) {
        this.type = type;
    }

    /**
     * 获取函数/工具名称
     *
     * @return 函数名称
     */
    public String getName() {
        return name;
    }

    /**
     * 设置函数/工具名称
     *
     * @param name 函数名称
     */
    public void setName(String name) {
        this.name = name;
    }

    /**
     * 获取函数参数
     *
     * @return JSON 字符串格式的参数
     */
    public String getArguments() {
        return arguments;
    }

    /**
     * 设置函数参数
     *
     * @param arguments JSON 字符串格式的参数
     */
    public void setArguments(String arguments) {
        this.arguments = arguments;
    }

    /**
     * 返回对象的字符串表示
     *
     * @return 对象的字符串表示
     */
    @Override
    public String toString() {
        return "ToolCall{" +
                "index=" + index +
                ", id='" + id + '\'' +
                ", type='" + type + '\'' +
                ", name='" + name + '\'' +
                ", arguments=" + arguments +
                '}';
    }
}