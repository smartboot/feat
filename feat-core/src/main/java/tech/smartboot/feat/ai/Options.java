package tech.smartboot.feat.ai;

public class Options {
    private String baseUrl = System.getenv("OPENAI_BASE_URL") != null ? System.getenv("OPENAI_BASE_URL") : "https://ai.gitee.com/v1/";
    private String model;
    private String apiKey = System.getenv("OPENAI_API_KEY");

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
}
