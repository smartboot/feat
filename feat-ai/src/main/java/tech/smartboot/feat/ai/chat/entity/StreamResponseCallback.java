package tech.smartboot.feat.ai.chat.entity;

public interface StreamResponseCallback extends ResponseCallback {
    default void onCompletion(ResponseMessage responseMessage) {
    }

    void onStreamResponse(String content);
}
