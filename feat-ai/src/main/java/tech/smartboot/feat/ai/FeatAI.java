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
