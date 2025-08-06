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
import tech.smartboot.feat.ai.chat.ModelVendor;
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
 * @author 三刀 zhengjunweimail@163.com
 * @version v1.0.0
 */
public class ProjectCoder extends BaseChat {
    public static void main(String[] args) throws IOException {

        ChatModel chatModel = FeatAI.chatModel(opts -> {
            opts.model(ModelVendor.Ollama.Qwen2_5_3B)
                    .debug(false);
        });

        File file1 = new File("./feat-core/src/main/java/tech/smartboot/feat/router");
        StringBuilder sources = new StringBuilder();
        Files.walkFileTree(file1.toPath(), new SimpleFileVisitor<Path>() {
            @Override
            public FileVisitResult visitFile(Path file, BasicFileAttributes attrs) throws IOException {
                readFile(file.toFile(), sources);
                return FileVisitResult.CONTINUE;
            }
        });

        chatModel.chatStream(PromptTemplate.PROJECT_CODER, data -> {
            data.put("reference", sources.toString());
            data.put("input", "分析下关于拦截器的路由匹配算法是否存在问题，然后给出优化方案");
        }, System.out::print);
    }
}
