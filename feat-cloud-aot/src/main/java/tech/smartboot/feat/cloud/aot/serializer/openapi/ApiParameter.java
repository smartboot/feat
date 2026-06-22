package tech.smartboot.feat.cloud.aot.serializer.openapi;

public class ApiParameter {
    private String name;
    private String in;
    private String type;
    private String refSchema;
    private boolean required;

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getIn() {
        return in;
    }

    public void setIn(String in) {
        this.in = in;
    }

    public String getType() {
        return type;
    }

    public void setType(String type) {
        this.type = type;
    }

    public String getRefSchema() {
        return refSchema;
    }

    public void setRefSchema(String refSchema) {
        this.refSchema = refSchema;
    }

    public boolean isRequired() {
        return required;
    }

    public void setRequired(boolean required) {
        this.required = required;
    }
}