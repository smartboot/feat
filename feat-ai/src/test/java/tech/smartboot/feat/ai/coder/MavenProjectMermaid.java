/*
 *  Copyright (C) [2024] smartboot [zhengjunweimail@163.com]
 *
 *  企业用户未经smartboot组织特别许可，需遵循AGPL-3.0开源协议合理合法使用本项目。
 *
 *   Enterprise users are required to use this project reasonably
 *   and legally in accordance with the AGPL-3.0 open source agreement
 *  without special permission from the smartboot organization.
 */

package tech.smartboot.feat.ai.coder;

import tech.smartboot.feat.ai.FeatAI;
import tech.smartboot.feat.ai.ModelMeta;
import tech.smartboot.feat.ai.chat.ChatModel;
import tech.smartboot.feat.ai.chat.prompt.PromptTemplate;
import tech.smartboot.feat.ai.demo.BaseChat;

import java.io.File;
import java.io.IOException;
import java.nio.file.FileVisitResult;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.SimpleFileVisitor;
import java.nio.file.attribute.BasicFileAttributes;

/**
 * @author 三刀(zhengjunweimail@163.com)
 * @version v1.0.0
 */
public class MavenProjectMermaid extends BaseChat {
    public static void main(String[] args) throws IOException {

        ChatModel chatModel = FeatAI.chatModel(opts -> {
            opts
//                    .model("qwen2.5:3b")
//                    .baseUrl("http://localhost:11434/v1") // Ollama本地服务地址
                    .model(ModelMeta.GITEE_AI_Qwen2_5_32B_Instruct)
                    .debug(true)
            ;
        });

        File file1 = new File("./");
        StringBuilder sourceBuilder = new StringBuilder();
        Files.walkFileTree(file1.toPath(), new SimpleFileVisitor<Path>() {
            @Override
            public FileVisitResult visitFile(Path file, BasicFileAttributes attrs) throws IOException {
                if (file.getFileName().toString().equals("pom.xml")) {
                    readFile(file.toFile(), sourceBuilder);
                    return FileVisitResult.CONTINUE;
                } else {
                    return FileVisitResult.SKIP_SUBTREE;
                }
            }
        });

        chatModel.chat(PromptTemplate.MAVEN_PROJECT_MERMAID, data -> {
            data.put("file_list", sourceBuilder.toString());
        }).whenComplete((responseMessage, throwable) -> {
            System.out.println(responseMessage.getContent());
        });


    }


}
