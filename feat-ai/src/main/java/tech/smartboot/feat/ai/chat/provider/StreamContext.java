package tech.smartboot.feat.ai.chat.provider;

import tech.smartboot.feat.ai.chat.entity.ChatResponse;
import tech.smartboot.feat.ai.chat.entity.Usage;

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
     * 流式终态响应
     * <p>provider 在终止标记（如 [DONE]/message_stop）处组装完成后写入，
     * 供上层在流结束后获取完整响应</p>
     */
    private volatile ChatResponse response;

    /**
     * 响应标识，同一流式响应内保持不变
     */
    private volatile String id;

    /**
     * 模型名称
     */
    private volatile String model;

    /**
     * 结束原因（如 stop、tool_calls）
     */
    private volatile String finishReason;

    /**
     * Token 用量统计，上游在流末尾返回
     */
    private volatile Usage usage;

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

    /**
     * 获取流式终态响应
     *
     * @return 终态响应，流未结束时为 null
     */
    public ChatResponse getResponse() {
        return response;
    }

    /**
     * 设置流式终态响应
     *
     * @param response 终态响应
     */
    public void setResponse(ChatResponse response) {
        this.response = response;
    }

    /**
     * 获取响应标识
     *
     * @return 响应标识，未收到时为 null
     */
    public String getId() {
        return id;
    }

    /**
     * 设置响应标识
     *
     * @param id 响应标识
     */
    public void setId(String id) {
        if (id != null) {
            this.id = id;
        }
    }

    /**
     * 获取模型名称
     *
     * @return 模型名称，未收到时为 null
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
        if (model != null) {
            this.model = model;
        }
    }

    /**
     * 获取结束原因
     *
     * @return 结束原因，未收到时为 null
     */
    public String getFinishReason() {
        return finishReason;
    }

    /**
     * 设置结束原因
     *
     * @param finishReason 结束原因
     */
    public void setFinishReason(String finishReason) {
        if (finishReason != null) {
            this.finishReason = finishReason;
        }
    }

    /**
     * 获取Token用量统计
     *
     * @return Token用量统计，未收到时为 null
     */
    public Usage getUsage() {
        return usage;
    }

    /**
     * 设置Token用量统计
     *
     * @param usage Token用量统计
     */
    public void setUsage(Usage usage) {
        if (usage != null) {
            this.usage = usage;
        }
    }
}
