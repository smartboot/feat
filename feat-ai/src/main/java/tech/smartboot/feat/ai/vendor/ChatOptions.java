/*
 *  Copyright (C) [2024] smartboot [zhengjunweimail@163.com]
 *
 *  企业用户未经smartboot组织特别许可，需遵循AGPL-3.0开源协议合理合法使用本项目。
 *
 *   Enterprise users are required to use this project reasonably
 *   and legally in accordance with the AGPL-3.0 open source agreement
 *  without special permission from the smartboot organization.
 */

package tech.smartboot.feat.ai.vendor;

import tech.smartboot.feat.ai.ModelMeta;
import tech.smartboot.feat.ai.Options;

/**
 * @author 三刀
 * @version v1.0 3/23/25
 */
public class ChatOptions extends Options {
    public static final ModelMeta DeepSeek_R1 = new ModelMeta(GiteeAI.BASE_URL, "DeepSeek-R1", false);
    public static final ModelMeta DeepSeek_R1_Distill_Qwen_32B = new ModelMeta(GiteeAI.BASE_URL, "DeepSeek-R1-Distill-Qwen-32B", false);
    public static final ModelMeta Qwen2_5_72B_Instruct = new ModelMeta(GiteeAI.BASE_URL, "Qwen2.5-72B-Instruct", true);
    public static final ModelMeta Qwen2_5_32B_Instruct = new ModelMeta(GiteeAI.BASE_URL, "Qwen2.5-32B-Instruct", false);
}
