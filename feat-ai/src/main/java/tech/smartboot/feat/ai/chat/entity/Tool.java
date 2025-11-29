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

/**
 * 工具类，表示AI模型可以调用的工具定义
 *
 * @author 三刀 zhengjunweimail@163.com
 * @version v1.0.0
 */
public class Tool {
    /**
     * 工具类型，如"function"
     */
    private String type;

    /**
     * 工具对应的函数定义
     */
    private Function function;

    /**
     * 获取工具类型
     *
     * @return 工具类型
     */
    public String getType() {
        return type;
    }

    /**
     * 设置工具类型
     *
     * @param type 工具类型
     */
    public void setType(String type) {
        this.type = type;
    }

    /**
     * 获取工具对应的函数定义
     *
     * @return 函数定义
     */
    public Function getFunction() {
        return function;
    }

    /**
     * 设置工具对应的函数定义
     *
     * @param function 函数定义
     */
    public void setFunction(Function function) {
        this.function = function;
    }
}