package tech.smartboot.feat.cloud.aot.serializer.doc;

import java.util.List;
import java.util.Map;

public class Operation {
    private String summary;
    private List<Parameter> parameters;
    private Map<String, Response> responses;

    public String getSummary() {
        return summary;
    }

    public void setSummary(String summary) {
        this.summary = summary;
    }

    public List<Parameter> getParameters() {
        return parameters;
    }

    public void setParameters(List<Parameter> parameters) {
        this.parameters = parameters;
    }

    public Map<String, Response> getResponses() {
        return responses;
    }

    public void setResponses(Map<String, Response> responses) {
        this.responses = responses;
    }
}