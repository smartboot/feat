package tech.smartboot.feat.ai.chat;

import com.alibaba.fastjson2.annotation.JSONField;

public class Choice {
    private int index;
    private ResponseMessage message;
    private String logprobs;
    @JSONField(name = "finish_reason")
    private String finishReason;
    @JSONField(name = "stop_reason")
    private String stopReason;

    public int getIndex() {
        return index;
    }

    public void setIndex(int index) {
        this.index = index;
    }

    public ResponseMessage getMessage() {
        return message;
    }

    public void setMessage(ResponseMessage message) {
        this.message = message;
    }

    public String getLogprobs() {
        return logprobs;
    }

    public void setLogprobs(String logprobs) {
        this.logprobs = logprobs;
    }

    public String getFinishReason() {
        return finishReason;
    }

    public void setFinishReason(String finishReason) {
        this.finishReason = finishReason;
    }

    public String getStopReason() {
        return stopReason;
    }

    public void setStopReason(String stopReason) {
        this.stopReason = stopReason;
    }
}
