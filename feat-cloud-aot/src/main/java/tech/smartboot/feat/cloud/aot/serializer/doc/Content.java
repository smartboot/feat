package tech.smartboot.feat.cloud.aot.serializer.doc;

public class Content {
    @com.alibaba.fastjson2.annotation.JSONField(name = "application/json")
    private MediaType applicationJson;

    public MediaType getApplicationJson() {
        return applicationJson;
    }

    public void setApplicationJson(MediaType applicationJson) {
        this.applicationJson = applicationJson;
    }
}