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
 * 聊天响应基础类，包含聊天API响应的基本信息
 *
 * @author 三刀 zhengjunweimail@163.com
 * @version v1.0.0
 */
public class ChatResponse {
    /**
     * 响应ID，唯一标识一次聊天请求的响应
     */
    private String id;

    /**
     * 对象类型，通常为"chat.completion"
     */
    private String object;

    /**
     * 创建时间戳，表示响应创建的时间
     */
    private long created;

    /**
     * 模型名称，表示生成该响应所使用的AI模型
     */
    private String model;


    /**
     * 获取响应ID
     *
     * @return 响应ID字符串
     */
    public String getId() {
        return id;
    }

    /**
     * 设置响应ID
     *
     * @param id 响应ID
     */
    public void setId(String id) {
        this.id = id;
    }

    /**
     * 获取对象类型
     *
     * @return 对象类型字符串
     */
    public String getObject() {
        return object;
    }

    /**
     * 设置对象类型
     *
     * @param object 对象类型
     */
    public void setObject(String object) {
        this.object = object;
    }

    /**
     * 获取创建时间戳
     *
     * @return 创建时间戳（秒）
     */
    public long getCreated() {
        return created;
    }

    /**
     * 设置创建时间戳
     *
     * @param created 创建时间戳
     */
    public void setCreated(long created) {
        this.created = created;
    }

    /**
     * 获取模型名称
     *
     * @return 模型名称字符串
     */
    public String getModel() {
        return model;
    }

    /**
     * 设置模型名称
     *
     * @param model 模型名称
     */
    public void setModel(String model) {
        this.model = model;
    }

}