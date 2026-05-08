package tech.smartboot.feat.ai.chat.provider;

import tech.smartboot.feat.ai.chat.ChatOptions;
import tech.smartboot.feat.ai.chat.entity.Message;
import tech.smartboot.feat.ai.chat.entity.ResponseMessage;
import tech.smartboot.feat.ai.chat.entity.StreamResponseCallback;

import java.util.List;
import java.util.function.Consumer;

/**
 * Chat API 处理器接口，用于定义不同 API 规范的处理逻辑
 */
public abstract class Provider {
    protected final ChatOptions options;
    public static final int STREAM_STATUS_INIT = 0;
    public static final int STREAM_STATUS_UPGRADE = 1;
    public static final int STREAM_STATUS_COMPLETE = 2;
    public static final int STREAM_STATUS_ERROR = 3;

    public Provider(ChatOptions options) {
        this.options = options;
    }

    /**
     * 处理流式响应
     */
    public abstract void chatStream(List<Message> messages, StreamResponseCallback consumer);

    /**
     * 处理非流式响应
     */
    public abstract void chat(List<Message> messages, Consumer<ResponseMessage> callback);
}
