package tech.smartboot.feat.ai.chat;

import tech.smartboot.feat.ai.chat.entity.ToolCall;

import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.atomic.AtomicInteger;

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
    // 工具调用累积器：key=index, value=ToolCall
    public Map<Integer, ToolCall> toolCallMap = new HashMap<>();
    // 文本内容累积器
    public StringBuilder contentBuilder = new StringBuilder();
    // 推理内容累积器
    public StringBuilder reasoningBuilder = new StringBuilder();
    /**
     * 流式响应状态：错误状态
     * <p>表示流式响应过程中发生错误</p>
     */
    public static final int STREAM_STATUS_ERROR = 3;
    private AtomicInteger status = new AtomicInteger(STREAM_STATUS_INIT);

    public int getStatus() {
        return status.get();
    }

    public void setStatus(int status) {
        this.status.set(status);
    }
}
