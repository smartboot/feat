package tech.smartboot.feat.cloud.aot.serializer.openapi;

public class RequestBody {
    private boolean required;
    private Content content;

    public boolean isRequired() {
        return required;
    }

    public void setRequired(boolean required) {
        this.required = required;
    }

    public Content getContent() {
        return content;
    }

    public void setContent(Content content) {
        this.content = content;
    }
}