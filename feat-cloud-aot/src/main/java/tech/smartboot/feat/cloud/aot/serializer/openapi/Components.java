package tech.smartboot.feat.cloud.aot.serializer.openapi;

import java.util.Map;

public class Components {
    private Map<String, Schema> schemas;

    public Map<String, Schema> getSchemas() {
        return schemas;
    }

    public void setSchemas(Map<String, Schema> schemas) {
        this.schemas = schemas;
    }
}
