/*
 *  Copyright (C) [2024] smartboot [zhengjunweimail@163.com]
 *
 *  企业用户未经smartboot组织特别许可，需遵循AGPL-3.0开源协议合理合法使用本项目。
 *
 *   Enterprise users are required to use this project reasonably
 *   and legally in accordance with the AGPL-3.0 open source agreement
 *  without special permission from the smartboot organization.
 */

package tech.smartboot.feat.ai.wechat;

import tech.smartboot.feat.ai.FeatAI;
import tech.smartboot.feat.ai.ModelMeta;
import tech.smartboot.feat.ai.chat.ChatModel;
import tech.smartboot.feat.ai.chat.entity.ResponseMessage;
import tech.smartboot.feat.ai.chat.entity.StreamResponseCallback;
import tech.smartboot.feat.ai.chat.prompt.PromptTemplate;
import tech.smartboot.feat.ai.demo.BaseChat;

import java.io.File;
import java.io.IOException;
import java.nio.file.FileVisitResult;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.SimpleFileVisitor;
import java.nio.file.attribute.BasicFileAttributes;
import java.util.Arrays;

/**
 * @author 三刀(zhengjunweimail@163.com)
 * @version v1.0.0
 */
public class WeChatEditor extends BaseChat {
    public static void main(String[] args) throws IOException {

        ChatModel chatModel = FeatAI.chatModel(opts -> {
            opts
//                    .model("qwen2.5:3b")
//                    .baseUrl("http://localhost:11434/v1") // Ollama本地服务地址
                    .model(ModelMeta.GITEE_AI_DeepSeek_R1_Distill_Qwen_32B)
//                    .debug(true)
            ;
        });

        StringBuilder sourceBuilder = new StringBuilder();
        //源码
        File sourcePath = new File("./feat-ai/src/main");
        sourceBuilder.append("---source begin---\n");
        Files.walkFileTree(sourcePath.toPath(), new SimpleFileVisitor<Path>() {
            @Override
            public FileVisitResult visitFile(Path file, BasicFileAttributes attrs) throws IOException {
                if (!file.getFileName().toString().endsWith(".html")) {
                    readFile(file.toFile(), sourceBuilder);
                }

                return FileVisitResult.CONTINUE;
            }

            @Override
            public FileVisitResult preVisitDirectory(Path dir, BasicFileAttributes attrs) throws IOException {
                if (Arrays.asList("audio", "embedding", "vector", "chat").contains(dir.getFileName().toString())) {
                    return FileVisitResult.SKIP_SUBTREE;
                }
                return super.preVisitDirectory(dir, attrs);
            }
        });
        sourceBuilder.append("---source end---\n");

        File demoPath = new File("./feat-ai/src/test/java");
        sourceBuilder.append("---demo begin---\n");
        Files.walkFileTree(demoPath.toPath(), new SimpleFileVisitor<Path>() {
            @Override
            public FileVisitResult visitFile(Path file, BasicFileAttributes attrs) throws IOException {
                readFile(file.toFile(), sourceBuilder);
                return FileVisitResult.CONTINUE;
            }

            @Override
            public FileVisitResult preVisitDirectory(Path dir, BasicFileAttributes attrs) throws IOException {
                if (Arrays.asList("demo", "test").contains(dir.getFileName().toString())) {
                    return FileVisitResult.SKIP_SUBTREE;
                }
                return FileVisitResult.CONTINUE;
            }
        });
        sourceBuilder.append("---demo end---\n");

        chatModel.chatStream(PromptTemplate.WECHAT_EDITOR, data -> {
            data.put("topic", "写一篇介绍Feat AI 关于提示词功能设计的文章。可以结合适当的代码和配置文件，介绍Feat AI 提示词的设计原理和使用方法。");
            data.put("reference", sourceBuilder.toString());
        }, new StreamResponseCallback() {
            @Override
            public void onStreamResponse(String content) {
                System.out.print(content);
            }

            @Override
            public void onCompletion(ResponseMessage responseMessage) {
                System.out.println(responseMessage.getContent());
                chatModel.chatStream("优化这篇文章:\n" + responseMessage.getContent(), new StreamResponseCallback() {
                    @Override
                    public void onStreamResponse(String content) {
                        System.out.print(content);
                    }

                    @Override
                    public void onCompletion(ResponseMessage responseMessage) {

                    }
                });
            }
        });


    }
}
