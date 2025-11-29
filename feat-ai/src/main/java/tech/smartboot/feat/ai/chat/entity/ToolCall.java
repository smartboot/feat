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

import java.util.Map;

/**
 * 工具调用类，表示AI模型对工具的调用信息
 *
 * @author 三刀 zhengjunweimail@163.com
 * @version v1.0.0
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
     * 函数调用信息，包含函数名和参数
     */
    private Map<String, String> function;

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
     * 获取函数调用信息
     *
     * @return 函数调用信息映射
     */
    public Map<String, String> getFunction() {
        return function;
    }

    /**
     * 设置函数调用信息
     *
     * @param function 函数调用信息映射
     */
    public void setFunction(Map<String, String> function) {
        this.function = function;
    }

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
                ", function=" + function +
                '}';
    }
}