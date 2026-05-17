package tech.smartboot.feat.ai.chat.entity;

import com.alibaba.fastjson2.JSONObject;

import java.util.List;

public class ImageMessage extends Message {
    private List<JSONObject> content;

    public List<JSONObject> getContent() {
        return content;
    }

    public void setContent(List<JSONObject> content) {
        this.content = content;
    }
}
