/*
 *  Copyright (C) [2024] smartboot [zhengjunweimail@163.com]
 *
 *  企业用户未经smartboot组织特别许可，需遵循Apache-2.0开源协议合理合法使用本项目。
 *
 *   Enterprise users are required to use this project reasonably
 *   and legally in accordance with the Apache-2.0 open source agreement
 *  without special permission from the smartboot organization.
 */

package tech.smartboot.feat.ai.chat;

import tech.smartboot.feat.ai.chat.entity.ResponseMessage;

/**
 * 流式响应回调接口，用于处理AI模型的流式输出数据
 * <p>
 * 该接口定义了处理大语言模型流式响应的标准回调方法。当与AI模型进行交互并启用流式响应时，
 * 模型会逐步返回生成的内容片段，而不是等待整个响应完成后一次性返回。
 * 通过实现此接口，可以实时接收和处理这些流式数据，提供更好的用户体验。
 * </p>
 *
 * <h3>主要用途：</h3>
 * <ul>
 *   <li>实时显示AI生成的文本内容</li>
 *   <li>处理推理过程中的中间结果</li>
 *   <li>监控流式响应的完成状态</li>
 *   <li>处理流式传输过程中可能出现的错误</li>
 * </ul>
 *
 * <h3>典型使用场景：</h3>
 * <pre>{@code
 * chatModel.chatStream("你好", new ChatStreamListener() {
 *     @Override
 *     public void onStreamResponse(String content) {
 *         // 实时处理每个文本片段
 *         System.out.print(content);
 *     }
 * 
 *     @Override
 *     public void onCompletion(ResponseMessage responseMessage) {
 *         // 处理完整的响应消息
 *         System.out.println("\n响应完成: " + responseMessage.getContent());
 *     }
 * });
 * }</pre>
 *
 * @author 三刀 zhengjunweimail@163.com
 * @version v1.0.0
 * @see ResponseMessage 完整的响应消息对象
 */
public interface ChatStreamListener {
    /**
     * 当流式响应完全结束时调用此方法
     * <p>
     * 此方法在所有的流式数据传输完成后被调用，提供完整的响应信息。
     * 可以在此处进行最终的清理工作或处理完整的响应结果。
     * </p>
     *
     * @param responseMessage 包含完整响应信息的消息对象，包括最终生成的文本、使用情况统计等
     */
    default void onCompletion(ResponseMessage responseMessage) {
    }

    /**
     * 处理模型推理过程中的中间内容
     * <p>
     * 当AI模型处于思考或推理阶段时，可能会产生一些中间的推理内容。
     * 此方法用于接收这些推理过程的片段，可用于展示模型的思考过程。
     * 注意：并非所有模型都支持推理内容的输出。
     * </p>
     *
     * @param content 推理过程的内容片段
     */
    default void onReasoning(String content) {

    }

    /**
     * 处理流式响应中的文本内容片段
     * <p>
     * 这是核心回调方法，每当AI模型生成新的文本片段时就会被调用。
     * 实现类应该在此方法中处理接收到的文本内容，例如实时更新UI显示。
     * </p>
     *
     * @param content 当前接收到的文本内容片段，可能是单个字符、单词或句子的一部分
     */
    void onStreamResponse(String content);

    /**
     * 处理流式响应过程中发生的错误
     * <p>
     * 当流式传输过程中出现异常时调用此方法。默认实现仅打印堆栈跟踪，
     * 建议在实际应用中重写此方法以提供更友好的错误处理机制。
     * </p>
     *
     * @param throwable 抛出的异常对象，包含错误详情
     */
    default void onError(Throwable throwable) {
        throwable.printStackTrace();
    }
}
