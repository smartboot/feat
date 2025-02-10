package tech.smartboot.feat.ai.chat;

public class WholeChoice extends Choice {

    /**
     * 非 stream 全量返回
     */
    private ResponseMessage message;

    public ResponseMessage getMessage() {
        return message;
    }

    public void setMessage(ResponseMessage message) {
        this.message = message;
    }
}
