package tech.smartboot.feat.ai.chat.entity;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

public class FunctionParameters {
    private String type = "object";
    private Map<String, ParameterProperty> properties = new HashMap<>();
    private Set<String> required = new HashSet<>();

    public String getType() {
        return type;
    }

    public void setType(String type) {
        this.type = type;
    }

    public Map<String, ParameterProperty> getProperties() {
        return properties;
    }


    public void addParameter(String name, String description, String type, boolean required) {
        this.properties.put(name, new ParameterProperty(type, description));
        if (required) {
            this.required.add(name);
        }
    }

    public Set<String> getRequired() {
        return required;
    }

    public void setRequired(Set<String> required) {
        this.required = required;
    }
}
