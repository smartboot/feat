package tech.smartboot.feat.cloud.aot.serializer.openapi;

public class Schema {
    private String type;
    private String format;
    private Schema items;
    @com.alibaba.fastjson2.annotation.JSONField(name = "$ref")
    private String ref;

    public String getType() {
        return type;
    }

    public void setType(String type) {
        this.type = type;
    }

    public String getFormat() {
        return format;
    }

    public void setFormat(String format) {
        this.format = format;
    }

    public Schema getItems() {
        return items;
    }

    public void setItems(Schema items) {
        this.items = items;
    }

    public String getRef() {
        return ref;
    }

    public void setRef(String ref) {
        this.ref = ref;
    }
}