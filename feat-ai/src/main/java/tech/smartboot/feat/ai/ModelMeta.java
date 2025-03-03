/*
 *  Copyright (C) [2024] smartboot [zhengjunweimail@163.com]
 *
 *  企业用户未经smartboot组织特别许可，需遵循AGPL-3.0开源协议合理合法使用本项目。
 *
 *   Enterprise users are required to use this project reasonably
 *   and legally in accordance with the AGPL-3.0 open source agreement
 *  without special permission from the smartboot organization.
 */

package tech.smartboot.feat.ai;

import java.util.HashMap;
import java.util.Map;

/**
 * @author 三刀(zhengjunweimail@163.com)
 * @version v1.0.0
 */
public class ModelMeta {
    private static final Map<String, Map<String, ModelMeta>> modelMetas = new HashMap<>();
    public static final ModelMeta GITEE_AI_DeepSeek_R1 = new ModelMeta(Options.AI_VENDOR_GITEE, "DeepSeek-R1", false);
    public static final ModelMeta GITEE_AI_DeepSeek_R1_Distill_Qwen_32B = new ModelMeta(Options.AI_VENDOR_GITEE, "DeepSeek-R1-Distill-Qwen-32B", false);
    public static final ModelMeta GITEE_AI_Qwen2_5_72B_Instruct = new ModelMeta(Options.AI_VENDOR_GITEE, "Qwen2.5-72B-Instruct", true);
    public static final ModelMeta GITEE_AI_Qwen2_5_32B_Instruct = new ModelMeta(Options.AI_VENDOR_GITEE, "Qwen2.5-32B-Instruct", false);

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
