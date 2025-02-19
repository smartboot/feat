package tech.smartboot.feat.ai.embedding;

import tech.smartboot.feat.ai.Options;

import java.util.HashMap;
import java.util.Map;

public class ModelVendor {
    private static final Map<String, Map<String, ModelVendor>> modelMetas = new HashMap<>();
    public static final ModelVendor GITEE_AI_BCE_BASE_V1 = new ModelVendor(tech.smartboot.feat.ai.Options.AI_VENDOR_GITEE, "bce-embedding-base_v1");
    public static final ModelVendor GITEE_AI_BGE_SMALL_ZH_V1_5 = new ModelVendor(tech.smartboot.feat.ai.Options.AI_VENDOR_GITEE, "bge-small-zh-v1.5");
    public static final ModelVendor GITEE_AI_BGE_LARGE_ZH_V1_5 = new ModelVendor(tech.smartboot.feat.ai.Options.AI_VENDOR_GITEE, "bge-large-zh-v1.5");
    public static final ModelVendor GITEE_AI_BGE_M3 = new ModelVendor(Options.AI_VENDOR_GITEE, "bge-m3");

    /**
     * 服务商
     */
    private final String vendor;
    /**
     * 模型名称
     */
    private final String model;


    public ModelVendor(String vendor, String model) {
        this.vendor = vendor;
        this.model = model;
        modelMetas.computeIfAbsent(vendor, k -> new HashMap<>()).putIfAbsent(model, this);
    }

    public String getVendor() {
        return vendor;
    }

    public String getModel() {
        return model;
    }


    public static ModelVendor get(String vendor, String model) {
        return modelMetas.getOrDefault(vendor, new HashMap<>()).getOrDefault(model, null);
    }

    public interface Ollama {
        String nomic_embed_text = "nomic-embed-text";
    }
}
