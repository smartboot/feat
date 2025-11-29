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
 * 完整选择项类，继承自Choice，包含完整的消息内容
 *
 * @author 三刀 zhengjunweimail@163.com
 * @version v1.0.0
 */
public class WholeChoice extends Choice {

    /**
     * 非 stream 全量返回的消息内容
     */
    private ResponseMessage message;

    /**
     * 获取响应消息
     *
     * @return 响应消息
     */
    public ResponseMessage getMessage() {
        return message;
    }

    /**
     * 设置响应消息
     *
     * @param message 响应消息
     */
    public void setMessage(ResponseMessage message) {
        this.message = message;
    }
}