package tech.smartboot.feat.ai.chat.entity;

public interface StreamResponseCallback extends ResponseCallback {
    void onStreamResponse(String content);
}
