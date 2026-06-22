package tech.smartboot.feat.cloud.aot.serializer.openapi;

public class Response {
    private String description;
    private Content content;

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public Content getContent() {
        return content;
    }

    public void setContent(Content content) {
        this.content = content;
    }
}