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

import tech.smartboot.feat.ai.agent.AgentOptions;
import tech.smartboot.feat.ai.agent.FeatAgent;
import tech.smartboot.feat.ai.chat.ChatModel;
import tech.smartboot.feat.ai.chat.ChatOptions;
import tech.smartboot.feat.ai.embedding.EmbeddingModel;
import tech.smartboot.feat.ai.embedding.EmbeddingOptions;
import tech.smartboot.feat.ai.reranker.RerankerModel;
import tech.smartboot.feat.ai.reranker.RerankerOptions;

import java.util.function.Consumer;

/**
 * @author 三刀 zhengjunweimail@163.com
 * @version v1.0.0
 */
public class FeatAI {
    public static ChatModel chatModel(Consumer<ChatOptions> consumer) {
        ChatOptions options = new ChatOptions();
        consumer.accept(options);
        return new ChatModel(options);
    }

    public static EmbeddingModel embedding(Consumer<EmbeddingOptions> consumer) {
        EmbeddingModel embeddingModel = new EmbeddingModel();
        consumer.accept(embeddingModel.options());
        return embeddingModel;
    }

    public static RerankerModel reranker(Consumer<RerankerOptions> consumer) {
        RerankerModel model = new RerankerModel();
        consumer.accept(model.options());
        return model;
    }

    public static FeatAgent agent(Consumer<AgentOptions> consumer) {
        AgentOptions options = new AgentOptions();
        consumer.accept(options);
//        return new ReActAgent(options);
        return null;
    }
}
