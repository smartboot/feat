package tech.smartboot.feat.ai.chat.entity;

import java.util.Map;

public class ToolCall {
    private int index;
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

    public int getIndex() {
        return index;
    }

    public void setIndex(int index) {
        this.index = index;
    }

    @Override
    public String toString() {
        return "ToolCall{" +
                "index=" + index +
                ", id='" + id + '\'' +
                ", type='" + type + '\'' +
                ", function=" + function +
                '}';
    }
}
