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
import java.util.concurrent.CompletableFuture;
import java.util.stream.Collectors;
import java.util.stream.Stream;

/**
 * 文件操作工具，提供文件系统操作功能
 * <p>
 * 该工具允许AI Agent执行常见的文件系统操作，包括：
 * 1. 列出目录内容
 * 2. 读取文件内容
 * 3. 写入文件内容
 * 4. 创建目录
 * 5. 删除文件
 * 6. 检查文件是否存在
 * </p>
 * <p>
 * 为了安全考虑，所有文件操作都被限制在指定的工作目录内，
 * 防止AI Agent访问系统敏感文件或目录。
 * </p>
 *
 * @author 三刀 zhengjunweimail@163.com
 * @version v1.0.0
 */
public class FileOperationTool implements AgentTool {

    private static final String NAME = "file_operation";
    private static final String DESCRIPTION = "提供文件系统操作功能，包括列出目录、读取文件、写入文件等";

    /**
     * 工作目录，限制文件操作在此目录内进行
     * <p>
     * 为了安全考虑，所有文件操作都被限制在该目录内，
     * 防止AI Agent访问系统敏感文件或目录。
     * </p>
     */
    private final String workingDirectory;

    /**
     * 全局排除模式列表
     * <p>
     * 定义一组文件或目录模式，这些模式匹配的路径在列出目录内容时会被忽略。
     * 默认排除node_modules、target、.git、.idea等开发相关目录。
     * </p>
     */
    private volatile List<String> globalExcludePatterns = new ArrayList<>(Arrays.asList("/node_modules/", "*/target/*", "/.git/", "/.idea/"));

    /**
     * 默认构造函数
     * <p>
     * 使用系统当前目录作为工作目录初始化工具。
     * </p>
     */
    public FileOperationTool() {
        this.workingDirectory = System.getProperty("user.dir");
    }

    /**
     * 带工作目录参数的构造函数
     *
     * @param workingDirectory 指定的工作目录路径
     */
    public FileOperationTool(String workingDirectory) {
        this.workingDirectory = workingDirectory;
    }

    /**
     * 设置全局排除模式
     * <p>
     * 允许自定义需要排除的文件或目录模式列表，替代默认的排除模式。
     * </p>
     *
     * @param patterns 要排除的文件或目录模式列表
     */
    public void setGlobalExcludePatterns(java.util.List<String> patterns) {
        this.globalExcludePatterns = new java.util.ArrayList<>(patterns != null ? patterns : java.util.Collections.emptyList());
    }

    /**
     * 获取当前全局排除模式
     *
     * @return 当前排除模式列表的副本
     */
    public java.util.List<String> getGlobalExcludePatterns() {
        return new java.util.ArrayList<>(this.globalExcludePatterns);
    }

    /**
     * 执行文件操作工具
     * <p>
     * 根据传入的参数执行相应的文件操作，支持多种操作类型：
     * 1. list_directory: 列出目录内容
     * 2. read_file: 读取文件内容
     * 3. write_file: 写入文件内容
     * 4. create_directory: 创建目录
     * 5. delete_file: 删除文件
     * 6. file_exists: 检查文件是否存在
     * </p>
     *
     * @param parameters 包含操作类型和相关参数的JSON对象
     * @return 操作结果字符串
     */
    @Override
    public CompletableFuture<String> execute(JSONObject parameters) {
        String action = parameters.getString("action");

        if (action == null) {
            return CompletableFuture.completedFuture("错误：必须提供'action'参数");
        }

        try {
            switch (action) {
                case "list_directory":
                    return CompletableFuture.completedFuture(listDirectory(parameters));
                case "read_file":
                    return CompletableFuture.completedFuture(readFile(parameters));
                case "write_file":
                    return CompletableFuture.completedFuture(writeFile(parameters));
                case "create_directory":
                    return CompletableFuture.completedFuture(createDirectory(parameters));
                case "delete_file":
                    return CompletableFuture.completedFuture(deleteFile(parameters));
                case "file_exists":
                    return CompletableFuture.completedFuture(fileExists(parameters));
                default:
                    return CompletableFuture.completedFuture("错误：不支持的操作 '" + action + "'");
            }
        } catch (Exception e) {
            return CompletableFuture.completedFuture("执行操作时出错: " + e.getMessage());
        }
    }

    /**
     * 获取工具名称
     *
     * @return 工具名称 "file_operation"
     */
    @Override
    public String getName() {
        return NAME;
    }

    /**
     * 获取工具描述
     *
     * @return 工具功能描述
     */
    @Override
    public String getDescription() {
        return DESCRIPTION;
    }

    /**
     * 获取工具参数的JSON Schema定义
     * <p>
     * 定义了该工具支持的所有操作及其参数格式，供AI Agent正确调用工具。
     * </p>
     *
     * @return 参数定义的JSON Schema字符串
     */
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
     * <p>
     * 根据参数列出指定目录的内容，支持递归和非递归两种模式。
     * 会过滤掉符合排除模式的文件或目录。
     * </p>
     *
     * @param parameters 包含路径和递归标志的参数
     * @return 目录内容列表字符串
     * @throws IOException IO操作异常
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
     * <p>
     * 读取指定文件的全部内容并返回。
     * </p>
     *
     * @param parameters 包含文件路径的参数
     * @return 文件内容字符串
     * @throws IOException IO操作异常
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
     * <p>
     * 将指定内容写入文件，如果文件不存在会自动创建，
     * 如果目录不存在也会自动创建目录。
     * </p>
     *
     * @param parameters 包含文件路径和内容的参数
     * @return 操作结果字符串
     * @throws IOException IO操作异常
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
     * <p>
     * 创建指定路径的目录，如果目录已存在则返回错误。
     * </p>
     *
     * @param parameters 包含目录路径的参数
     * @return 操作结果字符串
     * @throws IOException IO操作异常
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
     * <p>
     * 删除指定路径的文件或空目录。
     * </p>
     *
     * @param parameters 包含文件路径的参数
     * @return 操作结果字符串
     * @throws IOException IO操作异常
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
     * <p>
     * 检查指定路径的文件或目录是否存在。
     * </p>
     *
     * @param parameters 包含文件路径的参数
     * @return "true"表示存在，"false"表示不存在
     */
    private String fileExists(JSONObject parameters) {
        String pathStr = parameters.getString("path");

        Path path = resolvePath(pathStr);
        return String.valueOf(Files.exists(path));
    }

    /**
     * 解析并验证路径，确保在工作目录范围内
     * <p>
     * 将相对或绝对路径解析为工作目录内的有效路径，
     * 并验证路径安全性，防止路径遍历攻击。
     * </p>
     *
     * @param pathStr 原始路径字符串
     * @return 解析后的安全路径
     * @throws SecurityException 当路径超出工作目录范围时抛出
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
     * <p>
     * 根据排除模式列表判断指定路径是否应该被排除在操作结果之外。
     * </p>
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
     * <p>
     * 支持基本的通配符匹配，其中'*'匹配任意字符序列，'?'匹配单个字符。
     * </p>
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
