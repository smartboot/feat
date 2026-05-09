package tech.smartboot.feat.ai.chat;

import tech.smartboot.feat.ai.chat.entity.ResponseMessage;

public interface CompletionHandler {

    void completed(ResponseMessage message);


    default void failed(Throwable exc) {
        exc.printStackTrace();
    }
}
