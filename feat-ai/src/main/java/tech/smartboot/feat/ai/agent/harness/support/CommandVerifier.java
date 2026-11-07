/*
 *  Copyright (C) [2024] smartboot [zhengjunweimail@163.com]
 *
 *  企业用户未经smartboot组织特别许可，需遵循Apache-2.0开源协议合理合法使用本项目。
 */

package tech.smartboot.feat.ai.agent.harness.support;

import tech.smartboot.feat.ai.agent.harness.StepContext;
import tech.smartboot.feat.ai.agent.harness.Verifier;
import tech.smartboot.feat.ai.agent.harness.VerifyResult;

import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.Charset;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.TimeUnit;

/**
 * 命令式验证器：跑测试、跑 lint、跑构建，用**退出码**判定结论。
 * <p>
 * 这是"机械验证"最省事也最可靠的一种：不依赖模型自评，不依赖额外 LLM 调用，
 * 只需要一条能返回非零退出码的命令。典型用法：
 * </p>
 * <pre>{@code
 * Verifier verifier = CommandVerifier.of(projectDir)
 *         .add(Check.of("compile", "mvn -q -o -DskipTests compile").timeout(300_000))
 *         .add(Check.of("test", "mvn -q -o test").timeout(600_000));
 * }</pre>
 * <p>
 * 三个实现细节值得注意：
 * <ol>
 *     <li>输出必须在独立线程里消费。进程输出超过管道缓冲区（约 4KB）时，
 *         先 waitFor 再读必然死锁——这是 ProcessBuilder 最经典的坑；</li>
 *     <li>只保留输出尾部若干字符。测试失败信息通常在最后几十行，
 *         而把整份日志塞回模型既贵又没用；</li>
 *     <li>超时即失败，但反馈里要写明"超时"而不是"断言失败"，
 *         两者的修复动作完全不同。</li>
 * </ol>
 * </p>
 *
 * @author 三刀 zhengjunweimail@163.com
 * @version v0.1.0
 */
public class CommandVerifier implements Verifier {

    private static final boolean WINDOWS = System.getProperty("os.name", "")
            .toLowerCase().contains("win");

    /**
     * 子进程输出按控制台代码页解码。
     * <p>
     * 不能用默认字符集：应用里常见 {@code -Dfile.encoding=UTF-8}，而 Windows 控制台
     * 仍然按 OEM 代码页（简中为 GBK）输出，直接用默认字符集解码会得到乱码，
     * 后面"命令不存在"的中文特征串就匹配不上了。{@code sun.jnu.encoding}
     * 反映的才是操作系统本地编码。
     * </p>
     */
    private static final Charset CONSOLE_CHARSET = detectConsoleCharset();

    private final List<Check> checks = new ArrayList<Check>();
    private final String workDir;
    private final int maxOutputChars;

    private CommandVerifier(String workDir, int maxOutputChars) {
        this.workDir = workDir;
        this.maxOutputChars = maxOutputChars;
    }

    public static CommandVerifier of(String workDir) {
        return new CommandVerifier(workDir, 4_000);
    }

    public static CommandVerifier of(String workDir, int maxOutputChars) {
        return new CommandVerifier(workDir, Math.max(512, maxOutputChars));
    }

    public CommandVerifier add(Check check) {
        checks.add(check);
        return this;
    }

    public CommandVerifier addAll(Check... check) {
        checks.addAll(Arrays.asList(check));
        return this;
    }

    @Override
    public CompletableFuture<VerifyResult> verify(StepContext context, String answer) {
        if (checks.isEmpty()) {
            return CompletableFuture.completedFuture(VerifyResult.skip("command", "未配置检查项"));
        }
        // 命令执行会阻塞，绝不能跑在调用方线程上：它可能占用的是模型回调线程
        return CompletableFuture.supplyAsync(() -> runAll());
    }

    private VerifyResult runAll() {
        long start = System.currentTimeMillis();
        List<String> failures = new ArrayList<String>();
        int skipped = 0;
        for (Check check : checks) {
            CheckResult result = runOne(check);
            if (result.skipped) {
                skipped++;
            } else if (!result.ok) {
                failures.add("### " + check.name + "\n命令：" + check.command + "\n" + result.detail);
            }
        }
        long cost = System.currentTimeMillis() - start;
        if (skipped == checks.size()) {
            return VerifyResult.skip("command", "全部 " + checks.size() + " 项检查因环境原因跳过");
        }
        if (failures.isEmpty()) {
            return VerifyResult.pass("command", "全部 " + (checks.size() - skipped) + " 项检查通过", cost);
        }
        StringBuilder feedback = new StringBuilder();
        feedback.append("机械验证未通过（").append(failures.size()).append("/").append(checks.size())
                .append(" 项失败）。请修复后重新提交结论，不要只是重述你已经做了什么。\n\n");
        for (String failure : failures) {
            feedback.append(failure).append("\n\n");
        }
        return VerifyResult.fail("command", feedback.toString(), cost);
    }

    private CheckResult runOne(Check check) {
        Process process = null;
        StringBuilder buffer = new StringBuilder();
        try {
            ProcessBuilder builder = WINDOWS
                    ? new ProcessBuilder("cmd", "/c", check.command)
                    : new ProcessBuilder("/bin/sh", "-c", check.command);
            if (workDir != null && !workDir.isEmpty()) {
                builder.directory(new java.io.File(workDir));
            }
            builder.redirectErrorStream(true);
            process = builder.start();

            final Process target = process;
            Thread reader = new Thread(() -> drain(target, buffer), "feat-verifier-drain");
            reader.setDaemon(true);
            reader.start();

            boolean finished = process.waitFor(check.timeoutMillis, TimeUnit.MILLISECONDS);
            if (!finished) {
                process.destroyForcibly();
                process.waitFor(2, TimeUnit.SECONDS);
                reader.join(500);
                return CheckResult.fail("超时：超过 " + check.timeoutMillis + "ms 未结束，已强制终止。\n"
                        + "最后输出：\n" + tail(buffer));
            }
            reader.join(1_000);
            int exitCode = process.exitValue();
            if (exitCode == 0) {
                return CheckResult.pass();
            }
            // 127（sh）/ 9009（cmd）：命令或解释器不存在。这是环境缺件，不是答案错
            if (exitCode == 127 || exitCode == 9009) {
                return CheckResult.skip("命令不存在（退出码 " + exitCode + "），跳过该项检查");
            }
            String output = tail(buffer);
            if (looksLikeMissingCommand(output)) {
                return CheckResult.skip("命令不存在，跳过该项检查：" + firstLine(output));
            }
            return CheckResult.fail("退出码 " + exitCode + "。输出尾部：\n" + output);
        } catch (IOException e) {
            // 命令不存在 / 工作目录不合法：属于环境问题，不是答案问题，按弃权处理
            return CheckResult.skip("无法执行命令：" + e.getMessage());
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            return CheckResult.skip("验证被中断");
        } finally {
            if (process != null && process.isAlive()) {
                process.destroyForcibly();
            }
        }
    }

    private void drain(Process process, StringBuilder buffer) {
        byte[] chunk = new byte[8_192];
        try (InputStream in = process.getInputStream()) {
            int read;
            while ((read = in.read(chunk)) != -1) {
                synchronized (buffer) {
                    buffer.append(new String(chunk, 0, read, CONSOLE_CHARSET));
                    if (buffer.length() > maxOutputChars * 2) {
                        buffer.delete(0, buffer.length() - maxOutputChars);
                    }
                }
            }
        } catch (IOException ignored) {
            // 进程被强杀时流会抛错，忽略
        }
    }

    /**
     * 识别"命令不存在"的输出。
     * <p>
     * 为什么要做字符串匹配：cmd 与 sh 对"命令不存在"的退出码并不统一
     * （sh 是 127，cmd 视调用方式可能是 9009 也可能是 1），光看退出码会把
     * "环境没装 mvn" 误判成 "测试失败"，进而让模型去改一堆与任务无关的代码。
     * </p>
     */
    private boolean looksLikeMissingCommand(String output) {
        if (output == null) {
            return false;
        }
        String lower = output.toLowerCase();
        return lower.contains("is not recognized as an internal or external command")
                || output.contains("不是内部或外部命令")
                || lower.contains("command not found");
    }

    private static Charset detectConsoleCharset() {
        String name = System.getProperty("sun.jnu.encoding");
        if (name != null && !name.isEmpty()) {
            try {
                return Charset.forName(name);
            } catch (Exception ignored) {
                // 落到默认字符集
            }
        }
        return Charset.defaultCharset();
    }

    private String firstLine(String text) {
        if (text == null) {
            return "";
        }
        int index = text.indexOf('\n');
        return index < 0 ? text : text.substring(0, index);
    }

    private String tail(StringBuilder buffer) {        synchronized (buffer) {
            int length = buffer.length();
            if (length <= maxOutputChars) {
                return buffer.toString();
            }
            return "...（省略前 " + (length - maxOutputChars) + " 字符）\n"
                    + buffer.substring(length - maxOutputChars);
        }
    }

    /**
     * 一条检查项
     */
    public static final class Check {
        private final String name;
        private final String command;
        private long timeoutMillis = 300_000L;

        private Check(String name, String command) {
            this.name = name;
            this.command = command;
        }

        public static Check of(String name, String command) {
            return new Check(name, command);
        }

        public Check timeout(long timeoutMillis) {
            this.timeoutMillis = timeoutMillis;
            return this;
        }

        public String getName() {
            return name;
        }

        public String getCommand() {
            return command;
        }

        public long getTimeoutMillis() {
            return timeoutMillis;
        }
    }

    private static final class CheckResult {
        private final boolean ok;
        private final boolean skipped;
        private final String detail;

        private CheckResult(boolean ok, boolean skipped, String detail) {
            this.ok = ok;
            this.skipped = skipped;
            this.detail = detail;
        }

        private static CheckResult pass() {
            return new CheckResult(true, false, null);
        }

        private static CheckResult fail(String detail) {
            return new CheckResult(false, false, detail);
        }

        /**
         * 环境问题（命令不存在 / 无法执行）：不阻塞结论，但要和"真通过"区分开，
         * 否则一次环境缺件会被误读成"验证通过"
         */
        private static CheckResult skip(String detail) {
            return new CheckResult(true, true, detail);
        }
    }

    public List<Check> getChecks() {
        return Collections.unmodifiableList(checks);
    }
}
