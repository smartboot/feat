/*
 *  Copyright (C) [2024] smartboot [zhengjunweimail@163.com]
 *
 *  企业用户未经smartboot组织特别许可，需遵循AGPL-3.0开源协议合理合法使用本项目。
 *
 *   Enterprise users are required to use this project reasonably
 *   and legally in accordance with the AGPL-3.0 open source agreement
 *  without special permission from the smartboot organization.
 */

package tech.smartboot.feat.ai.demo;

import tech.smartboot.feat.core.common.exception.FeatException;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.SimpleFileVisitor;
import java.util.Arrays;
import java.util.Collections;
import java.util.Set;

public class BaseChat {

    public static String toHtml(String content) {
        return content.replace("&", "&amp;").replace("<", "&lt;").replace(">", "&gt;").replace("\n", "<br/>").replace("\r", "<br/>").replace(" ", "&nbsp;");
    }

    public static void loadFileNames(File file, StringBuilder sb) {
        for (File f : file.listFiles()) {
            if (f.getName().startsWith(".")) {
                continue;
            }
            if (f.isDirectory()) {
                if (Arrays.asList("node_modules", ".idea", ".git", "target").contains(f.getName())) {
                    continue;
                }
                loadFileNames(f, sb);
            }
            if (f.isFile()) {
                try {
                    sb.append(f.getCanonicalFile()).append("\n");
                } catch (IOException e) {
                    throw new FeatException(e);
                }
            }
        }
    }


    public static void loadFile(File file, StringBuilder sb) throws IOException {
        loadFile(file, new SimpleFileVisitor<Path>() {
        }, sb);
    }

    public static void loadFile(File file, SimpleFileVisitor<Path> visitor, StringBuilder sb) throws IOException {
        for (Path path : Files.walkFileTree(file.toPath(), visitor)) {
            sb.append("## ").append(path.getFileName()).append("\n");
            try (FileInputStream fis = new FileInputStream(path.toFile());) {
                byte[] bytes = new byte[1024];
                int len;
                while ((len = fis.read(bytes)) != -1) {
                    sb.append(new String(bytes, 0, len));
                }
                sb.append("\n");
            }
        }
    }

    public static void readFile(File file,StringBuilder sb) throws IOException {
        sb.append("## ").append(file.getCanonicalFile()).append("\n");
        try (FileInputStream fis = new FileInputStream(file)) {
            byte[] bytes = new byte[1024];
            int len;
            while ((len = fis.read(bytes)) != -1) {
                sb.append(new String(bytes, 0, len));
            }
            sb.append("\n");
        }
    }

    public static void loadSource(File file, StringBuilder sb) throws IOException {
        loadSource(file, Collections.emptySet(), sb);
    }

    public static void loadSource(File file, Set<String> ignoreDirName, StringBuilder sb) throws IOException {
        for (File f : file.listFiles()) {
            if (f.isDirectory()) {
                if (ignoreDirName.contains(f.getName())) {
                    continue;
                }
                loadSource(f, ignoreDirName, sb);
            } else if (f.isFile() && (f.getName().endsWith(".java") || f.getName().endsWith(".go"))) {
                sb.append("## " + f.getName() + "\n");
                sb.append("```\n");
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
