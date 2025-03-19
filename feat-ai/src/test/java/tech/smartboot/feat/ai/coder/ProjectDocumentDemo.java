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
import tech.smartboot.feat.ai.demo.BaseChat;
import tech.smartboot.feat.ai.chat.prompt.PromptTemplate;

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
public class ProjectDocumentDemo extends BaseChat {
    public static void main(String[] args) throws IOException {
        //文档
        StringBuilder docs = new StringBuilder();
        Files.walkFileTree(new File("./pages/src/content/docs/server/").toPath(), new SimpleFileVisitor<Path>() {
            @Override
            public FileVisitResult visitFile(Path file, BasicFileAttributes attrs) throws IOException {
                if (file.getFileName().toString().endsWith(".mdx")) {
                    readFile(file.toFile(), docs);
                }
                return FileVisitResult.CONTINUE;
            }
        });

        //源码
        StringBuilder source = new StringBuilder();
        Files.walkFileTree(new File("./feat-core/src/main/java/tech/smartboot/feat/fileserver").toPath(), new SimpleFileVisitor<Path>() {
            @Override
            public FileVisitResult visitFile(Path file, BasicFileAttributes attrs) throws IOException {
                readFile(file.toFile(), source);
                return FileVisitResult.CONTINUE;
            }
        });


        ChatModel chatModel = FeatAI.chatModel(opts -> {
            opts.model(ModelMeta.GITEE_AI_DeepSeek_R1_Distill_Qwen_32B)
                    .debug(false)
            ;
        });


        chatModel.chatStream(PromptTemplate.PROJECT_DOCUMENT_EDITOR, data -> {
            data.put("input", "用Mermaid画一张介绍fileserver的架构图或者流程图，各环节要尽量细致、准确");
            data.put("ref_source", source.toString());
            data.put("ref_doc", docs.toString());
        }, System.out::print);
    }
}
