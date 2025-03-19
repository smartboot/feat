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
 * 流式响应回调接口，用于处理流式数据的响应
 * <p>
 * 该接口继承自ResponseCallback，专门用于处理流式数据的场景。它允许在接收到流式数据时进行实时处理，
 * 并在数据传输完成时执行相应的完成操作。
 *
 * @author 三刀(zhengjunweimail@163.com)
 * @version v1.0.0
 */
public interface StreamResponseCallback extends ResponseCallback {
    /**
     * 当流式响应完成时调用此方法
     * <p>
     * 此方法提供了一个默认的空实现，子类可以根据需要选择是否重写该方法。
     *
     * @param responseMessage 响应消息对象，包含完整的响应信息
     */
    default void onCompletion(ResponseMessage responseMessage) {
    }

    /**
     * 处理流式响应数据
     * <p>
     * 当收到流式数据片段时，会调用此方法。实现类应该在此方法中处理接收到的数据内容。
     *
     * @param content 接收到的数据内容片段
     */
    void onStreamResponse(String content);
}
