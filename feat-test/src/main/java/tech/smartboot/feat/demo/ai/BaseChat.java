package tech.smartboot.feat.demo.ai;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.util.Collections;
import java.util.Set;

public class BaseChat {

    public static String toHtml(String content) {
        return content.replace("&", "&amp;").replace("<", "&lt;").replace(">", "&gt;").replace("\n", "<br/>").replace("\r", "<br/>").replace(" ", "&nbsp;");
    }

    public static void loadFile(File file, StringBuilder sb) throws IOException {
        loadFile(file, Collections.emptySet(), sb);
    }

    public static void loadFile(File file, Set<String> ignore, StringBuilder sb) throws IOException {
        for (File f : file.listFiles()) {
            if (f.isDirectory()) {
                if (ignore.contains(f.getName())) {
                    continue;
                }
                loadFile(f, sb);
            }
            if (f.isFile() && f.getName().endsWith(".mdx")) {
                sb.append("## ").append(f.getName()).append("\n");
                try (FileInputStream fis = new FileInputStream(f);) {
                    byte[] bytes = new byte[1024];
                    int len;
                    while ((len = fis.read(bytes)) != -1) {
                        sb.append(new String(bytes, 0, len));
                    }
                    sb.append("\n");
                }
            }
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
