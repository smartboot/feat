/*
 *  Copyright (C) [2024] smartboot [zhengjunweimail@163.com]
 *
 *  企业用户未经smartboot组织特别许可，需遵循Apache-2.0开源协议合理合法使用本项目。
 *
 *   Enterprise users are required to use this project reasonably
 *   and legally in accordance with the Apache-2.0 open source agreement
 *  without special permission from the smartboot organization.
 */

package tech.smartboot.feat.demo.featclaw.tools;

import com.alibaba.fastjson2.JSONObject;
import tech.smartboot.feat.ai.agent.AgentTool;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.stream.Collectors;
import java.util.stream.Stream;

/**
 * 项目分析工具 - 为 FeatClaw Agent 提供项目结构分析能力
 * <p>
 * 该工具允许 AI Agent 分析 Maven 项目的结构和依赖关系，支持：
 * 1. 分析项目目录结构
 * 2. 读取并解析 pom.xml
 * 3. 分析 Java 源代码结构
 * 4. 统计代码行数和文件数量
 * 5. 识别项目中的关键类和接口
 * </p>
 *
 * @author Feat Team
 * @version v1.0.0
 */
public class ProjectAnalyzerTool implements AgentTool {

    private static final String NAME = "project_analyzer";
    private static final String DESCRIPTION = "分析项目结构，包括目录结构、pom.xml、源代码统计等。帮助理解项目架构和依赖关系。";

    /**
     * 工作目录
     */
    private final String workingDirectory;

    /**
     * 默认构造函数
     */
    public ProjectAnalyzerTool() {
        this.workingDirectory = System.getProperty("user.dir");
    }

    /**
     * 带工作目录参数的构造函数
     *
     * @param workingDirectory 指定的工作目录路径
     */
    public ProjectAnalyzerTool(String workingDirectory) {
        this.workingDirectory = workingDirectory;
    }

    @Override
    public CompletableFuture<String> execute(JSONObject parameters) {
        String action = parameters.getString("action");

        if (action == null) {
            return CompletableFuture.completedFuture("错误：必须提供 'action' 参数");
        }

        try {
            switch (action) {
                case "analyze_structure":
                    return CompletableFuture.completedFuture(analyzeStructure(parameters));
                case "read_pom":
                    return CompletableFuture.completedFuture(readPom(parameters));
                case "list_source_files":
                    return CompletableFuture.completedFuture(listSourceFiles(parameters));
                case "count_code_lines":
                    return CompletableFuture.completedFuture(countCodeLines(parameters));
                case "analyze_dependencies":
                    return CompletableFuture.completedFuture(analyzeDependencies(parameters));
                default:
                    return CompletableFuture.completedFuture("错误：不支持的操作 '" + action + "'");
            }
        } catch (Exception e) {
            return CompletableFuture.completedFuture("执行操作时出错: " + e.getMessage());
        }
    }

    /**
     * 分析项目目录结构
     */
    private String analyzeStructure(JSONObject parameters) throws IOException {
        String subPath = parameters.getString("path");
        int maxDepth = parameters.getIntValue("max_depth", 3);

        Path targetPath = subPath != null 
            ? Paths.get(workingDirectory, subPath)
            : Paths.get(workingDirectory);

        if (!Files.exists(targetPath)) {
            return "错误：路径不存在: " + targetPath;
        }

        StringBuilder result = new StringBuilder();
        result.append("项目结构分析:\n");
        result.append("根路径: ").append(targetPath.toAbsolutePath()).append("\n");
        result.append("========================================\n");

        try (Stream<Path> paths = Files.walk(targetPath, maxDepth)) {
            List<Path> sortedPaths = paths
                .filter(p -> !p.toString().contains("/target/"))
                .filter(p -> !p.toString().contains("/.git/"))
                .filter(p -> !p.toString().contains("/node_modules/"))
                .sorted()
                .collect(Collectors.toList());

            for (Path path : sortedPaths) {
                int depth = targetPath.relativize(path).getNameCount();
                StringBuilder indentBuilder = new StringBuilder();
                for (int i = 0; i < depth; i++) {
                    indentBuilder.append("  ");
                }
                String indent = indentBuilder.toString();
                String name = path.getFileName() != null ? path.getFileName().toString() : targetPath.toString();
                
                if (Files.isDirectory(path)) {
                    result.append(indent).append("📁 ").append(name).append("/\n");
                } else {
                    String icon = getFileIcon(name);
                    result.append(indent).append(icon).append(" ").append(name);
                    try {
                        long size = Files.size(path);
                        result.append(" (").append(formatFileSize(size)).append(")");
                    } catch (IOException ignored) {}
                    result.append("\n");
                }
            }
        }

        return result.toString();
    }

    /**
     * 读取 pom.xml 内容
     */
    private String readPom(JSONObject parameters) throws IOException {
        String modulePath = parameters.getString("module_path");
        
        Path pomPath = modulePath != null
            ? Paths.get(workingDirectory, modulePath, "pom.xml")
            : Paths.get(workingDirectory, "pom.xml");

        if (!Files.exists(pomPath)) {
            return "错误：找不到 pom.xml: " + pomPath;
        }

        String content = new String(Files.readAllBytes(pomPath), StandardCharsets.UTF_8);
        
        // 提取关键信息
        StringBuilder result = new StringBuilder();
        result.append("POM 文件分析: ").append(pomPath).append("\n");
        result.append("========================================\n\n");
        
        // 提取 artifactId
        String artifactId = extractXmlTag(content, "artifactId");
        if (artifactId != null) {
            result.append("Artifact ID: ").append(artifactId).append("\n");
        }
        
        // 提取 groupId
        String groupId = extractXmlTag(content, "groupId");
        if (groupId != null) {
            result.append("Group ID: ").append(groupId).append("\n");
        }
        
        // 提取 version
        String version = extractXmlTag(content, "version");
        if (version != null) {
            result.append("Version: ").append(version).append("\n");
        }
        
        // 提取 packaging
        String packaging = extractXmlTag(content, "packaging");
        if (packaging != null) {
            result.append("Packaging: ").append(packaging).append("\n");
        }
        
        // 提取依赖列表
        result.append("\n主要依赖:\n");
        int depStart = content.indexOf("<dependencies>");
        int depEnd = content.indexOf("</dependencies>");
        if (depStart != -1 && depEnd != -1) {
            String depsSection = content.substring(depStart, depEnd);
            // 简单提取 artifactId 列表
            int idx = 0;
            while ((idx = depsSection.indexOf("<artifactId>", idx)) != -1) {
                int endIdx = depsSection.indexOf("</artifactId>", idx);
                if (endIdx != -1) {
                    String dep = depsSection.substring(idx + 12, endIdx);
                    result.append("  - ").append(dep).append("\n");
                    idx = endIdx;
                } else {
                    break;
                }
            }
        }

        result.append("\n完整 POM 内容:\n");
        result.append(content);

        return result.toString();
    }

    /**
     * 列出源代码文件
     */
    private String listSourceFiles(JSONObject parameters) throws IOException {
        String subPath = parameters.getString("path");
        String extension = parameters.getString("extension"); // java, xml, properties
        int limit = parameters.getIntValue("limit", 100);

        Path sourcePath = subPath != null
            ? Paths.get(workingDirectory, subPath)
            : Paths.get(workingDirectory, "src");

        if (!Files.exists(sourcePath)) {
            return "错误：源代码路径不存在: " + sourcePath;
        }

        StringBuilder result = new StringBuilder();
        result.append("源代码文件列表:\n");
        result.append("路径: ").append(sourcePath.toAbsolutePath()).append("\n");
        result.append("========================================\n");

        try (Stream<Path> paths = Files.walk(sourcePath)) {
            List<Path> files = paths
                .filter(Files::isRegularFile)
                .filter(p -> extension == null || p.toString().endsWith("." + extension))
                .filter(p -> !p.toString().contains("/target/"))
                .limit(limit)
                .collect(Collectors.toList());

            result.append("找到 ").append(files.size()).append(" 个文件");
            if (files.size() >= limit) {
                result.append(" (仅显示前 ").append(limit).append(" 个)");
            }
            result.append("\n\n");

            for (Path file : files) {
                String relativePath = sourcePath.relativize(file).toString();
                result.append("  📄 ").append(relativePath);
                try {
                    long size = Files.size(file);
                    result.append(" (").append(formatFileSize(size)).append(")");
                } catch (IOException ignored) {}
                result.append("\n");
            }
        }

        return result.toString();
    }

    /**
     * 统计代码行数
     */
    private String countCodeLines(JSONObject parameters) throws IOException {
        String subPath = parameters.getString("path");
        Path targetPath = subPath != null
            ? Paths.get(workingDirectory, subPath)
            : Paths.get(workingDirectory, "src");

        if (!Files.exists(targetPath)) {
            return "错误：路径不存在: " + targetPath;
        }

        int totalFiles = 0;
        int totalLines = 0;
        long totalSize = 0;

        try (Stream<Path> paths = Files.walk(targetPath)) {
            List<Path> files = paths
                .filter(Files::isRegularFile)
                .filter(p -> p.toString().endsWith(".java"))
                .filter(p -> !p.toString().contains("/target/"))
                .collect(Collectors.toList());

            for (Path file : files) {
                totalFiles++;
                totalSize += Files.size(file);
                List<String> lines = Files.readAllLines(file, StandardCharsets.UTF_8);
                // 过滤空行
                int nonEmptyLines = 0;
                for (String line : lines) {
                    if (!line.trim().isEmpty()) {
                        nonEmptyLines++;
                    }
                }
                totalLines += nonEmptyLines;
            }
        }

        StringBuilder result = new StringBuilder();
        result.append("代码统计:\n");
        result.append("========================================\n");
        result.append("Java 文件数量: ").append(totalFiles).append("\n");
        result.append("代码行数（不含空行）: ").append(totalLines).append("\n");
        result.append("总文件大小: ").append(formatFileSize(totalSize)).append("\n");
        if (totalFiles > 0) {
            result.append("平均每文件行数: ").append(totalLines / totalFiles).append("\n");
        }

        return result.toString();
    }

    /**
     * 分析项目依赖
     */
    private String analyzeDependencies(JSONObject parameters) {
        StringBuilder result = new StringBuilder();
        result.append("依赖分析:\n");
        result.append("========================================\n");
        result.append("注意：此工具提供静态分析，完整的依赖分析建议使用 Maven 命令。\n\n");
        result.append("建议执行以下 Maven 命令获取完整依赖信息:\n");
        result.append("  mvn dependency:tree\n");
        result.append("  mvn dependency:analyze\n");
        result.append("  mvn dependency:list\n");

        return result.toString();
    }

    /**
     * 从 XML 内容中提取标签值
     */
    private String extractXmlTag(String content, String tagName) {
        String openTag = "<" + tagName + ">";
        String closeTag = "</" + tagName + ">";
        int start = content.indexOf(openTag);
        int end = content.indexOf(closeTag);
        if (start != -1 && end != -1 && end > start) {
            return content.substring(start + openTag.length(), end).trim();
        }
        return null;
    }

    /**
     * 获取文件图标
     */
    private String getFileIcon(String fileName) {
        if (fileName.endsWith(".java")) return "☕";
        if (fileName.endsWith(".xml")) return "📋";
        if (fileName.endsWith(".properties")) return "⚙️";
        if (fileName.endsWith(".md")) return "📝";
        if (fileName.endsWith(".txt")) return "📄";
        if (fileName.endsWith(".json")) return "🔧";
        if (fileName.endsWith(".yml") || fileName.endsWith(".yaml")) return "⚙️";
        if (fileName.endsWith(".sql")) return "🗄️";
        return "📄";
    }

    /**
     * 格式化文件大小
     */
    private String formatFileSize(long size) {
        if (size < 1024) return size + " B";
        if (size < 1024 * 1024) return String.format("%.2f KB", size / 1024.0);
        return String.format("%.2f MB", size / (1024.0 * 1024));
    }

    @Override
    public String getName() {
        return NAME;
    }

    @Override
    public String getDescription() {
        return DESCRIPTION;
    }

    @Override
    public String getParametersSchema() {
        return "{\n" +
            "  \"type\": \"object\",\n" +
            "  \"properties\": {\n" +
            "    \"action\": {\n" +
            "      \"type\": \"string\",\n" +
            "      \"enum\": [\"analyze_structure\", \"read_pom\", \"list_source_files\", \"count_code_lines\", \"analyze_dependencies\"],\n" +
            "      \"description\": \"要执行的分析操作类型\"\n" +
            "    },\n" +
            "    \"path\": {\n" +
            "      \"type\": \"string\",\n" +
            "      \"description\": \"要分析的路径（相对路径）\"\n" +
            "    },\n" +
            "    \"module_path\": {\n" +
            "      \"type\": \"string\",\n" +
            "      \"description\": \"模块路径（用于读取子模块的 pom.xml）\"\n" +
            "    },\n" +
            "    \"max_depth\": {\n" +
            "      \"type\": \"integer\",\n" +
            "      \"default\": 3,\n" +
            "      \"description\": \"目录遍历最大深度\"\n" +
            "    },\n" +
            "    \"extension\": {\n" +
            "      \"type\": \"string\",\n" +
            "      \"description\": \"文件扩展名过滤\"\n" +
            "    },\n" +
            "    \"limit\": {\n" +
            "      \"type\": \"integer\",\n" +
            "      \"default\": 100,\n" +
            "      \"description\": \"最大返回文件数量\"\n" +
            "    }\n" +
            "  },\n" +
            "  \"required\": [\"action\"]\n" +
            "}";
    }
}
