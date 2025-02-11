package tech.smartboot.feat.demo.ai;

import tech.smartboot.feat.ai.FeatAI;
import tech.smartboot.feat.ai.ModelMeta;
import tech.smartboot.feat.ai.chat.ChatModel;
import tech.smartboot.feat.ai.chat.entity.ResponseMessage;
import tech.smartboot.feat.ai.chat.entity.StreamResponseCallback;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;

public class ProjectDocumentDemo {
    public static void main(String[] args) throws IOException {
        File file = new File("pages/src/content");
        StringBuilder docs = new StringBuilder();
        loadFile(file, docs);

        StringBuilder sourceBuilder = new StringBuilder();
        loadSource(new File("feat-core/src/main/java/tech/smartboot/feat/ai"), sourceBuilder);

        StringBuilder demoBuilder = new StringBuilder();
        loadSource(new File("feat-test/src/main/java/tech/smartboot/feat/demo/ai"), demoBuilder);
        ChatModel chatModel = FeatAI.chatModel(opts -> {
            opts.model(ModelMeta.GITEE_AI_DeepSeek_R1_Distill_Qwen_32B)
                    .system("你主要负责为这个项目编写使用文档，根据用户要求编写相关章节内容。" +
                            "参考内容为：\n" + docs
                            + "\n 实现源码为：\n" + sourceBuilder
                            + "\n 示例代码为：" + demoBuilder)
            ;
        });
        chatModel.chatStream("写一篇关于 Fead AI的基本介绍，主要围绕关键接口的基本使用，不需要详细示例。", new StreamResponseCallback() {

            @Override
            public void onCompletion(ResponseMessage responseMessage) {

            }

            @Override
            public void onStreamResponse(String content) {
                System.out.print(content);
            }
        });
    }

    public static void loadFile(File file, StringBuilder sb) throws IOException {
        for (File f : file.listFiles()) {
            if (f.isDirectory()) {
                loadFile(f, sb);
            }
            if (f.isFile() && f.getName().endsWith(".mdx")) {
                try (FileInputStream fis = new FileInputStream(f);) {
                    byte[] bytes = new byte[1024];
                    int len;
                    while ((len = fis.read(bytes)) != -1) {
                        sb.append(new String(bytes, 0, len));
                    }
                }
            }
        }
    }

    public static void loadSource(File file, StringBuilder sb) throws IOException {
        for (File f : file.listFiles()) {
//            if (f.isDirectory()) {
//                loadFile(f, sb);
//            }
            if (f.isFile() && f.getName().endsWith(".java")) {
                sb.append("## " + f.getName() + "\n");
                sb.append("```java\n");
                try (FileInputStream fis = new FileInputStream(f);) {
                    byte[] bytes = new byte[1024];
                    int len;
                    while ((len = fis.read(bytes)) != -1) {
                        sb.append(new String(bytes, 0, len));
                    }
                }
                sb.append("\n```\n");
            }
        }
    }
}
