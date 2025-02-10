package tech.smartboot.feat.demo.ai;

import tech.smartboot.feat.ai.FeatAI;
import tech.smartboot.feat.ai.chat.ChatModel;

import java.io.IOException;

public class ChatDemo {
    public static void main(String[] args) throws IOException {
        ChatModel chatModel = FeatAI.chatModel(opts -> opts.model("Qwen2.5-72B-Instruct"));
        chatModel.chat("你好，请自我介绍一下。", rsp -> {
            System.out.println("rsp: " + rsp.getContent());
            System.out.println("usage: " + rsp.getUsage());
        });
    }
}
