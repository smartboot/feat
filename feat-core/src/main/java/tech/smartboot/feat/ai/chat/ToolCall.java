package tech.smartboot.feat.ai.chat;

import java.util.Map;

public class ToolCall {
    private String id;
    private String type;
    private Map<String, String> function;

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public String getType() {
        return type;
    }

    public void setType(String type) {
        this.type = type;
    }

    public Map<String, String> getFunction() {
        return function;
    }

    public void setFunction(Map<String, String> function) {
        this.function = function;
    }

}
