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
 * 响应格式类，定义AI模型响应的格式要求
 *
 * @author 三刀 zhengjunweimail@163.com
 * @version v1.0 3/23/25
 */
public class ResponseFormat {
    /**
     * JSON格式响应常量实例
     */
    public static final ResponseFormat JSON = new ResponseFormat("json_object");

    /**
     * 响应格式类型
     */
    private String type;

    /**
     * 构造函数
     *
     * @param type 响应格式类型
     */
    public ResponseFormat(String type) {
        this.type = type;
    }

    /**
     * 获取响应格式类型
     *
     * @return 响应格式类型
     */
    public String getType() {
        return type;
    }

    /**
     * 设置响应格式类型
     *
     * @param type 响应格式类型
     */
    public void setType(String type) {
        this.type = type;
    }
}