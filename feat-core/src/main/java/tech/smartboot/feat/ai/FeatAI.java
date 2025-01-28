package tech.smartboot.feat.ai;

import tech.smartboot.feat.ai.chat.ChatModel;

import java.util.function.Consumer;

public class FeatAI {
    public static ChatModel chatModel(Consumer<Options> consumer) {
        Options options = new Options();
        consumer.accept(options);
        return new ChatModel(options);
    }
}
