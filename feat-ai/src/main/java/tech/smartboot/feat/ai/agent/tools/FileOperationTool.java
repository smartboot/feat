/*
 *  Copyright (C) [2024] smartboot [zhengjunweimail@163.com]
 *
 *  企业用户未经smartboot组织特别许可，需遵循AGPL-3.0开源协议合理合法使用本项目。
 *
 *   Enterprise users are required to use this project reasonably
 *   and legally in accordance with the AGPL-3.0 open source agreement
 *  without special permission from the smartboot organization.
 */

package tech.smartboot.feat.ai.agent.tools;

import com.alibaba.fastjson2.JSONObject;
import tech.smartboot.feat.ai.agent.AgentTool;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.Stream;

/**
 * 文件操作工具，提供文件系统操作功能
 *
 * @author 三刀 zhengjunweimail@163.com
 * @version v1.0.0
 */
public class FileOperationTool implements AgentTool {

    private static final String NAME = "file_operation";
    private static final String DESCRIPTION = "提供文件系统操作功能，包括列出目录、读取文件、写入文件等";

    // 工作目录，限制文件操作在此目录内进行
    private final String workingDirectory;

    // 全局排除模式列表
    private volatile List<String> globalExcludePatterns = new ArrayList<>(Arrays.asList("/node_modules/", "*/target/*", "/.git/", "/.idea/"));

    public FileOperationTool() {
        this.workingDirectory = System.getProperty("user.dir");
    }

    public FileOperationTool(String workingDirectory) {
        this.workingDirectory = workingDirectory;
    }

    /**
     * 设置全局排除模式
     *
     * @param patterns 要排除的文件或目录模式列表
     */
    public void setGlobalExcludePatterns(java.util.List<String> patterns) {
        this.globalExcludePatterns = new java.util.ArrayList<>(patterns != null ? patterns : java.util.Collections.emptyList());
    }

    /**
     * 获取当前全局排除模式
     *
     * @return 当前排除模式列表
     */
    public java.util.List<String> getGlobalExcludePatterns() {
        return new java.util.ArrayList<>(this.globalExcludePatterns);
    }

    @Override
    public String execute(JSONObject parameters) {
        String action = parameters.getString("action");

        if (action == null) {
            return "错误：必须提供'action'参数";
        }

        try {
            switch (action) {
                case "list_directory":
                    return listDirectory(parameters);
                case "read_file":
                    return readFile(parameters);
                case "write_file":
                    return writeFile(parameters);
                case "create_directory":
                    return createDirectory(parameters);
                case "delete_file":
                    return deleteFile(parameters);
                case "file_exists":
                    return fileExists(parameters);
                default:
                    return "错误：不支持的操作 '" + action + "'";
            }
        } catch (Exception e) {
            return "执行操作时出错: " + e.getMessage();
        }
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
                "      \"description\": \"操作类型: list_directory, read_file, write_file, create_directory, delete_file, file_exists\",\n" +
                "      \"enum\": [\"list_directory\", \"read_file\", \"write_file\", \"create_directory\", \"delete_file\", \"file_exists\"]\n" +
                "    },\n" +
                "    \"path\": {\n" +
                "      \"type\": \"string\",\n" +
                "      \"description\": \"文件或目录的路径\"\n" +
                "    },\n" +
                "    \"content\": {\n" +
                "      \"type\": \"string\",\n" +
                "      \"description\": \"要写入文件的内容\"\n" +
                "    },\n" +
                "    \"recursive\": {\n" +
                "      \"type\": \"boolean\",\n" +
                "      \"description\": \"是否递归列出目录内容\"\n" +
                "    }\n" +
                "  },\n" +
                "  \"required\": [\"action\", \"path\"]\n" +
                "}";
    }

    /**
     * 列出目录内容
     */
    private String listDirectory(JSONObject parameters) throws IOException {
        String pathStr = parameters.getString("path");
        boolean recursive = parameters.getBooleanValue("recursive", false);

        // 使用全局排除模式
        java.util.List<String> excludePatterns = this.globalExcludePatterns;

        Path path = resolvePath(pathStr);
        if (!Files.exists(path)) {
            return "错误：路径不存在: " + pathStr;
        }

        if (!Files.isDirectory(path)) {
            return "错误：指定路径不是目录: " + pathStr;
        }

        if (recursive) {
            try (Stream<Path> paths = Files.walk(path)) {
                return paths
                        .filter(p -> !shouldExclude(p, path, excludePatterns))
                        .map(p -> p.toString().substring(workingDirectory.length()))
                        .collect(Collectors.joining("\n"));
            }
        } else {
            try (Stream<Path> paths = Files.list(path)) {
                return paths
                        .filter(p -> !shouldExclude(p, path, excludePatterns))
                        .map(p -> p.toString().substring(workingDirectory.length()))
                        .collect(Collectors.joining("\n"));
            }
        }
    }

    /**
     * 读取文件内容
     */
    private String readFile(JSONObject parameters) throws IOException {
        String pathStr = parameters.getString("path");

        Path path = resolvePath(pathStr);
        if (!Files.exists(path)) {
            return "错误：文件不存在: " + pathStr;
        }

        if (!Files.isRegularFile(path)) {
            return "错误：指定路径不是文件: " + pathStr;
        }

        // Use Files.lines to read file content as a string
        try (Stream<String> lines = Files.lines(path)) {
            return lines.collect(Collectors.joining("\n"));
        }
    }

    /**
     * 写入文件内容
     */
    private String writeFile(JSONObject parameters) throws IOException {
        String pathStr = parameters.getString("path");
        String content = parameters.getString("content");

        if (content == null) {
            return "错误：必须提供'content'参数";
        }

        Path path = resolvePath(pathStr);

        // 创建父目录（如果不存在）
        Path parent = path.getParent();
        if (parent != null && !Files.exists(parent)) {
            Files.createDirectories(parent);
        }

        // Write string content to file using BufferedWriter
        try (java.io.BufferedWriter writer = Files.newBufferedWriter(path)) {
            writer.write(content);
            writer.flush();
        }
        return "已成功写入文件: " + pathStr;
    }

    /**
     * 创建目录
     */
    private String createDirectory(JSONObject parameters) throws IOException {
        String pathStr = parameters.getString("path");

        Path path = resolvePath(pathStr);
        if (Files.exists(path)) {
            return "错误：路径已存在: " + pathStr;
        }

        Files.createDirectories(path);
        return "已成功创建目录: " + pathStr;
    }

    /**
     * 删除文件
     */
    private String deleteFile(JSONObject parameters) throws IOException {
        String pathStr = parameters.getString("path");

        Path path = resolvePath(pathStr);
        if (!Files.exists(path)) {
            return "错误：路径不存在: " + pathStr;
        }

        Files.delete(path);
        return "已成功删除: " + pathStr;
    }

    /**
     * 检查文件是否存在
     */
    private String fileExists(JSONObject parameters) {
        String pathStr = parameters.getString("path");

        Path path = resolvePath(pathStr);
        return String.valueOf(Files.exists(path));
    }

    /**
     * 解析并验证路径，确保在工作目录范围内
     */
    private Path resolvePath(String pathStr) {
        Path path = Paths.get(pathStr).normalize();
        Path resolvedPath;

        if (path.isAbsolute()) {
            // 如果是绝对路径，确保它在工作目录内
            resolvedPath = path;
        } else {
            // 如果是相对路径，解析为相对于工作目录的绝对路径
            resolvedPath = Paths.get(workingDirectory).resolve(path).normalize();
        }

        // 确保路径在工作目录内（防止路径遍历攻击）
        Path workingDirPath = Paths.get(workingDirectory).normalize().toAbsolutePath();
        Path absolutePath = resolvedPath.toAbsolutePath();

        if (!absolutePath.startsWith(workingDirPath)) {
            throw new SecurityException("访问被拒绝：路径必须在工作目录内: " + pathStr);
        }

        return resolvedPath;
    }

    /**
     * 检查路径是否应该被排除
     *
     * @param path            要检查的路径
     * @param basePath        基础路径
     * @param excludePatterns 排除模式列表
     * @return 如果应该排除返回true，否则返回false
     */
    private boolean shouldExclude(Path path, Path basePath, java.util.List<String> excludePatterns) {
        // 如果没有排除模式，则不排除任何内容
        if (excludePatterns == null || excludePatterns.isEmpty()) {
            return false;
        }

        // 获取相对路径
        Path relativePath = basePath.relativize(path);
        String relativePathStr = relativePath.toString();

        // 检查每个排除模式
        for (String pattern : excludePatterns) {
            // 处理简单的通配符模式
            if (matchesPattern(relativePathStr, pattern)) {
                return true;
            }

            // 检查文件名是否匹配模式
            String fileName = path.getFileName().toString();
            if (matchesPattern(fileName, pattern)) {
                return true;
            }
        }

        return false;
    }

    /**
     * 简单的通配符模式匹配
     *
     * @param text    要检查的文本
     * @param pattern 模式（支持*和?通配符）
     * @return 如果匹配返回true，否则返回false
     */
    private boolean matchesPattern(String text, String pattern) {
        // 将通配符模式转换为正则表达式
        StringBuilder regex = new StringBuilder();
        for (char c : pattern.toCharArray()) {
            switch (c) {
                case '*':
                    regex.append(".*");
                    break;
                case '?':
                    regex.append(".");
                    break;
                case '.':
                case '$':
                case '^':
                case '[':
                case ']':
                case '(':
                case ')':
                case '{':
                case '}':
                case '|':
                case '\\':
                    regex.append('\\').append(c);
                    break;
                default:
                    regex.append(c);
                    break;
            }
        }

        return java.util.regex.Pattern.matches(regex.toString(), text);
    }
}
