package tech.smartboot.feat.cloud.aot.serializer.doc;

import java.util.ArrayList;
import java.util.List;

public class ApiEndpoint {
    private String path;
    private String description;
    private String responseType;
    private List<String> methods = new ArrayList<>();
    private List<ApiParameter> parameters = new ArrayList<>();

    public String getPath() {
        return path;
    }

    public void setPath(String path) {
        this.path = path;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public String getResponseType() {
        return responseType;
    }

    public void setResponseType(String responseType) {
        this.responseType = responseType;
    }

    public List<String> getMethods() {
        return methods;
    }

    public void setMethods(List<String> methods) {
        this.methods = methods;
    }

    public List<ApiParameter> getParameters() {
        return parameters;
    }

    public void setParameters(List<ApiParameter> parameters) {
        this.parameters = parameters;
    }
}
