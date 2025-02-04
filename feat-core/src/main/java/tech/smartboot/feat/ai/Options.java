package tech.smartboot.feat.ai;

import tech.smartboot.feat.ai.chat.Function;

import java.util.HashMap;
import java.util.Map;

public class Options {
    private String baseUrl = System.getenv("OPENAI_BASE_URL") != null ? System.getenv("OPENAI_BASE_URL") : "https://ai.gitee.com/v1/";
    private String model;
    private String apiKey = System.getenv("OPENAI_API_KEY");
    private String system;
    private boolean debug;
    private Map<String, Function> functions = new HashMap<>();

    public String baseUrl() {
        return baseUrl;
    }

    public Options baseUrl(String baseUrl) {
        this.baseUrl = baseUrl;
        return this;
    }

    public String getModel() {
        return model;
    }

    public Options model(String model) {
        this.model = model;
        return this;
    }

    public String getApiKey() {
        return apiKey;
    }

    public Options apiKey(String apiKey) {
        this.apiKey = apiKey;
        return this;
    }

    public Map<String, Function> functions() {
        return functions;
    }

    public Options addFunction(Function function) {
        this.functions.put(function.getName(), function);
        return this;
    }

    public String getSystem() {
        return system;
    }

    public Options system(String system) {
        this.system = system;
        return this;
    }

    public boolean isDebug() {
        return debug;
    }

    public Options debug(boolean debug) {
        this.debug = debug;
        return this;
    }
}
