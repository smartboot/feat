package tech.smartboot.feat.ai.chat;

import java.util.concurrent.atomic.AtomicInteger;

/**
 * 流式响应上下文类
 * <p>用于管理AI流式响应的状态和数据累积</p>
 *
 * @author Feat Team
 */
public class StreamContext {

    /**
     * 流式响应状态：初始化状态
     * <p>表示流式请求刚开始，尚未收到任何数据</p>
     */
    public static final int STREAM_STATUS_INIT = 0;

    /**
     * 流式响应状态：升级状态
     * <p>表示已成功建立流式连接，开始接收数据片段</p>
     */
    public static final int STREAM_STATUS_UPGRADE = 1;

    /**
     * 流式响应状态：完成状态
     * <p>表示流式响应已正常结束，收到终止标记</p>
     */
    public static final int STREAM_STATUS_COMPLETE = 2;

    /**
     * 流式响应状态：错误状态
     * <p>表示流式响应过程中发生错误</p>
     */
    public static final int STREAM_STATUS_ERROR = 3;

    /**
     * 文本内容累积器
     */
    private final StringBuilder contentBuilder = new StringBuilder();

    /**
     * 推理内容累积器
     */
    private final StringBuilder reasoningBuilder = new StringBuilder();

    /**
     * 当前流式响应状态
     */
    private final AtomicInteger status = new AtomicInteger(STREAM_STATUS_INIT);

    /**
     * 获取当前流式响应状态
     *
     * @return 当前状态值
     */
    public int getStatus() {
        return status.get();
    }

    /**
     * 设置流式响应状态
     *
     * @param status 新的状态值
     */
    public void setStatus(int status) {
        this.status.set(status);
    }

    /**
     * 获取文本内容构建器
     *
     */
    public void appendContent(String content) {
        contentBuilder.append(content);
    }

    /**
     * 获取推理内容构建器
     *
     */
    public void appendReasoning(String reasoning) {
        reasoningBuilder.append(reasoning);
    }

    public String getContent() {
        return contentBuilder.toString();
    }

    public String getReasoning() {
        return reasoningBuilder.toString();
    }
}
