/*
 *  Copyright (C) [2024] smartboot [zhengjunweimail@163.com]
 *
 *  企业用户未经smartboot组织特别许可，需遵循AGPL-3.0开源协议合理合法使用本项目。
 *
 *   Enterprise users are required to use this project reasonably
 *   and legally in accordance with the AGPL-3.0 open source agreement
 *  without special permission from the smartboot organization.
 */

package tech.smartboot.feat.ai.chat;

import com.alibaba.fastjson2.JSONObject;

/**
 * 请求预处理接口，用于在发送请求前对请求参数进行预处理
 *
 * @author 三刀
 * @version v1.0 8/8/25
 */
public interface PreRequest {
    /**
     * 请求预处理方法，在发送请求前调用
     *
     * @param chatModel   聊天模型实例
     * @param modelVendor 模型供应商
     * @param jsonObject  请求参数JSON对象
     */
    void preRequest(ChatModel chatModel, ChatModelVendor modelVendor, JSONObject jsonObject);
}