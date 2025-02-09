package tech.smartboot.feat.ai;

import java.util.HashMap;
import java.util.Map;

public class ModelMeta {
    private static final Map<String, Map<String, ModelMeta>> modelMetas = new HashMap<>();
    public static final ModelMeta GITEE_AI_DeepSeek_R1_Distill_Qwen_32B = new ModelMeta(Options.AI_VENDOR_GITEE, "DeepSeek-R1-Distill-Qwen-32B", false);
    public static final ModelMeta GITEE_AI_Qwen2_5_72B_Instruct = new ModelMeta(Options.AI_VENDOR_GITEE, "Qwen2.5-72B-Instruct", true);

    /**
     * 服务商
     */
    private final String vendor;
    /**
     * 模型名称
     */
    private final String model;
    /**
     * 是否支持工具调用
     */
    private final boolean toolSupport;

    public ModelMeta(String vendor, String model, boolean toolSupport) {
        this.vendor = vendor;
        this.model = model;
        this.toolSupport = toolSupport;
        modelMetas.computeIfAbsent(vendor, k -> new HashMap<>()).putIfAbsent(model, this);
    }

    public String getVendor() {
        return vendor;
    }

    public String getModel() {
        return model;
    }

    public boolean isToolSupport() {
        return toolSupport;
    }

    public static ModelMeta get(String vendor, String model) {
        return modelMetas.getOrDefault(vendor, new HashMap<>()).getOrDefault(model, null);
    }
}
