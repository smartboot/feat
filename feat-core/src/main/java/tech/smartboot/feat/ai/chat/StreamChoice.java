package tech.smartboot.feat.ai.chat;

public class StreamChoice extends Choice {
    /**
     * stream 增量返回
     */
    private ResponseMessage delta;

    public ResponseMessage getDelta() {
        return delta;
    }

    public void setDelta(ResponseMessage delta) {
        this.delta = delta;
    }
}
