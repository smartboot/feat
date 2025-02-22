package tech.smartboot.feat.ai.chat.entity;

public interface ResponseCallback {
    void onCompletion(ResponseMessage responseMessage);
}
