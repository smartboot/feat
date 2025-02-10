package tech.smartboot.feat.ai.chat;

public interface StreamResponseCallback {
    void onStreamResponse(String content);

    void onStreamEnd(ResponseMessage responseMessage);

}
