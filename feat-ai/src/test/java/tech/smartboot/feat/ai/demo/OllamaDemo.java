package tech.smartboot.feat.ai.demo;

import tech.smartboot.feat.ai.FeatAI;
import tech.smartboot.feat.ai.chat.ChatModel;
import tech.smartboot.feat.ai.chat.entity.ResponseMessage;
import tech.smartboot.feat.ai.chat.entity.StreamResponseCallback;

public class OllamaDemo {
    public static void main(String[] args) {
        // 初始化Feat AI
        ChatModel chatModel = FeatAI.chatModel(opts -> {
            opts.model("deepseek-r1:32b")
                    .baseUrl("http://localhost:11434/v1") // Ollama本地服务地址
                    .system("你是一个擅长生成藏头诗的诗人。")
                    .debug(false);
        });

        // 用户输入的藏头关键词
        String[] keywords = {"情", "人", "节", "快", "乐"};

        // 向AI发送请求
        chatModel.chatStream(
                "根据以下关键词生成一首藏头诗：" + String.join(",", keywords),
                new StreamResponseCallback() {
                    @Override
                    public void onStreamResponse(String content) {
                        System.out.print(content);
                    }

                    @Override
                    public void onCompletion(ResponseMessage responseMessage) {
                        System.out.println("\n生成完成！");
                    }
                }
        );
    }
}
