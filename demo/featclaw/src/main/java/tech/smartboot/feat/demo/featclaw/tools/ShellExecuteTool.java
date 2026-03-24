/*
 *  Copyright (C) [2024] smartboot [zhengjunweimail@163.com]
 *
 *  企业用户未经smartboot组织特别许可，需遵循AGPL-3.0开源协议合理合法使用本项目。
 *
 *   Enterprise users are required to use this project reasonably
 *   and legally in accordance with the AGPL-3.0 open source agreement
 *  without special permission from the smartboot organization.
 */

package tech.smartboot.feat.demo.featclaw.tools;

import com.alibaba.fastjson2.JSONObject;
import tech.smartboot.feat.ai.agent.AgentTool;

import java.io.BufferedReader;
import java.io.File;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.TimeUnit;

/**
 * Shell 命令执行工具 - 为 FeatClaw Agent 提供命令执行能力
 * <p>
 * 该工具允许 AI Agent 安全地执行 Shell 命令，支持：
 * 1. 执行 Maven 命令（mvn clean install, mvn test 等）
 * 2. 执行 Git 命令（git status, git log 等）
 * 3. 执行文件操作命令（ls, cat, find 等）
 * 4. 执行其他安全的系统命令
 * </p>
 * <p>
 * 为了安全考虑，该工具实现了命令白名单机制和超时控制。
 * 只允许执行预定义的安全命令，且执行有时间限制。
 * </p>
 *
 * @author Feat Team
 * @version v1.0.0
 */
public class ShellExecuteTool implements AgentTool {

    private static final String NAME = "shell_execute";
    private static final String DESCRIPTION = "安全地执行 Shell 命令，支持 Maven、Git、文件操作等常用命令。具有命令白名单和超时控制机制。";

    /**
     * 允许执行的命令白名单
     */
    private static final List<String> ALLOWED_COMMANDS = Arrays.asList(
        // Maven 命令
        "mvn", "mvnw",
        // Git 命令
        "git",
        // Java 命令
        "java", "javac",
        // 文件操作命令
        "ls", "dir", "cat", "type", "find", "grep", "wc",
        // 目录操作
        "pwd", "cd", "mkdir", "rm", "cp", "mv",
        // 文本处理
        "head", "tail", "less", "more", "sort", "uniq",
        // 系统信息
        "echo", "date", "whoami",
        // 其他
        "tree", "stat", "file"
    );

    /**
     * 危险的命令和参数，禁止执行
     */
    private static final List<String> DANGEROUS_PATTERNS = Arrays.asList(
        "rm -rf /", "rm -rf /*", "> /dev/sda", "dd if=", "mkfs.",
        "format c:", "del /f /s /q c:", "rd /s /q c:",
        "poweroff", "reboot", "shutdown", "halt",
        ":(){ :|:& };:", // fork bomb
        "wget", "curl", // 防止下载执行恶意脚本
        "bash -c", "sh -c", // 防止命令注入
        "|", ";", "&&", "||" // 管道和逻辑操作符
    );

    /**
     * 工作目录
     */
    private final String workingDirectory;

    /**
     * 默认超时时间（秒）
     */
    private final int defaultTimeoutSeconds;

    /**
     * 默认构造函数
     */
    public ShellExecuteTool() {
        this.workingDirectory = System.getProperty("user.dir");
        this.defaultTimeoutSeconds = 60;
    }

    /**
     * 带参数的构造函数
     *
     * @param workingDirectory 工作目录
     * @param timeoutSeconds 默认超时时间（秒）
     */
    public ShellExecuteTool(String workingDirectory, int timeoutSeconds) {
        this.workingDirectory = workingDirectory;
        this.defaultTimeoutSeconds = timeoutSeconds;
    }

    @Override
    public CompletableFuture<String> execute(JSONObject parameters) {
        String command = parameters.getString("command");
        String workingDir = parameters.getString("working_dir");
        int timeout = parameters.getIntValue("timeout_seconds", defaultTimeoutSeconds);

        if (command == null || command.trim().isEmpty()) {
            return CompletableFuture.completedFuture("错误：必须提供 'command' 参数");
        }

        // 安全检查
        String securityCheck = checkCommandSecurity(command);
        if (securityCheck != null) {
            return CompletableFuture.completedFuture("安全警告: " + securityCheck);
        }

        // 执行命令
        try {
            String execDir = workingDir != null ? workingDir : this.workingDirectory;
            return executeCommand(command, execDir, timeout);
        } catch (Exception e) {
            return CompletableFuture.completedFuture("执行命令时出错: " + e.getMessage());
        }
    }

    /**
     * 检查命令安全性
     */
    private String checkCommandSecurity(String command) {
        String lowerCommand = command.toLowerCase().trim();

        // 检查危险模式
        for (String pattern : DANGEROUS_PATTERNS) {
            if (lowerCommand.contains(pattern.toLowerCase())) {
                return "命令包含危险模式 '" + pattern + "'，已被阻止执行";
            }
        }

        // 提取命令名（第一个单词）
        String cmdName = lowerCommand.split("\\s+")[0];
        
        // 检查是否在白名单中
        boolean allowed = false;
        for (String allowedCmd : ALLOWED_COMMANDS) {
            if (cmdName.equals(allowedCmd.toLowerCase())) {
                allowed = true;
                break;
            }
        }

        if (!allowed) {
            return "命令 '" + cmdName + "' 不在允许执行的命令列表中";
        }

        return null; // 通过安全检查
    }

    /**
     * 执行命令
     */
    private CompletableFuture<String> executeCommand(String command, String workingDir, int timeoutSeconds) {
        return CompletableFuture.supplyAsync(() -> {
            try {
                // 构建进程
                List<String> cmdList = new ArrayList<>();
                
                // 检测操作系统
                String os = System.getProperty("os.name").toLowerCase();
                if (os.contains("win")) {
                    // Windows
                    cmdList.add("cmd");
                    cmdList.add("/c");
                } else {
                    // Linux/Mac
                    cmdList.add("/bin/sh");
                    cmdList.add("-c");
                }
                cmdList.add(command);

                ProcessBuilder pb = new ProcessBuilder(cmdList);
                pb.directory(new File(workingDir));
                pb.redirectErrorStream(true); // 合并错误输出到标准输出

                // 启动进程
                Process process = pb.start();

                // 读取输出
                StringBuilder output = new StringBuilder();
                try (BufferedReader reader = new BufferedReader(
                        new InputStreamReader(process.getInputStream()))) {
                    String line;
                    while ((line = reader.readLine()) != null) {
                        output.append(line).append("\n");
                        // 限制输出大小
                        if (output.length() > 100000) {
                            output.append("\n...[输出已截断，超过 100KB]...");
                            break;
                        }
                    }
                }

                // 等待进程完成（带超时）
                boolean finished = process.waitFor(timeoutSeconds, TimeUnit.SECONDS);
                
                StringBuilder result = new StringBuilder();
                result.append("命令执行结果:\n");
                result.append("========================================\n");
                result.append("命令: ").append(command).append("\n");
                result.append("工作目录: ").append(workingDir).append("\n");
                result.append("超时设置: ").append(timeoutSeconds).append(" 秒\n");
                
                if (!finished) {
                    process.destroyForcibly();
                    result.append("状态: 超时（已强制终止）\n");
                } else {
                    int exitCode = process.exitValue();
                    result.append("退出码: ").append(exitCode).append("\n");
                    result.append("状态: ").append(exitCode == 0 ? "成功" : "失败").append("\n");
                }
                
                result.append("========================================\n");
                result.append("输出:\n");
                result.append(output.toString());

                return result.toString();

            } catch (Exception e) {
                return "执行命令时发生异常: " + e.getMessage();
            }
        });
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
            "    \"command\": {\n" +
            "      \"type\": \"string\",\n" +
            "      \"description\": \"要执行的 Shell 命令\"\n" +
            "    },\n" +
            "    \"working_dir\": {\n" +
            "      \"type\": \"string\",\n" +
            "      \"description\": \"命令执行的工作目录（默认为当前工作目录）\"\n" +
            "    },\n" +
            "    \"timeout_seconds\": {\n" +
            "      \"type\": \"integer\",\n" +
            "      \"default\": 60,\n" +
            "      \"description\": \"命令执行超时时间（秒）\"\n" +
            "    }\n" +
            "  },\n" +
            "  \"required\": [\"command\"]\n" +
            "}";
    }
}
