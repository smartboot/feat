package tech.smartboot.feat.ai.chat;

public interface StreamResponseCallback extends ResponseCallback {
    void onStreamResponse(String content);
}
