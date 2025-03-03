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

import tech.smartboot.feat.ai.chat.ChatModel;
import tech.smartboot.feat.ai.embedding.EmbeddingModel;
import tech.smartboot.feat.ai.embedding.EmbeddingOptions;

import java.util.function.Consumer;

public class FeatAI {
    public static ChatModel chatModel(Consumer<Options> consumer) {
        Options options = new Options();
        consumer.accept(options);
        return new ChatModel(options);
    }

    public static EmbeddingModel embedding(Consumer<EmbeddingOptions> consumer) {
        EmbeddingModel embeddingModel = new EmbeddingModel();
        consumer.accept(embeddingModel.options());
        return embeddingModel;
    }
}
