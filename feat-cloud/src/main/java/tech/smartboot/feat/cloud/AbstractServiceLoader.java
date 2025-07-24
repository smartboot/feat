/*
 *  Copyright (C) [2024] smartboot [zhengjunweimail@163.com]
 *
 *  企业用户未经smartboot组织特别许可，需遵循AGPL-3.0开源协议合理合法使用本项目。
 *
 *   Enterprise users are required to use this project reasonably
 *   and legally in accordance with the AGPL-3.0 open source agreement
 *  without special permission from the smartboot organization.
 */

package tech.smartboot.feat.cloud;

import com.alibaba.fastjson2.JSON;
import com.alibaba.fastjson2.JSONObject;
import tech.smartboot.feat.ai.mcp.model.ResourceTemplate;
import tech.smartboot.feat.ai.mcp.server.McpOptions;
import tech.smartboot.feat.ai.mcp.server.McpServer;
import tech.smartboot.feat.ai.mcp.server.model.ServerPrompt;
import tech.smartboot.feat.ai.mcp.server.model.ServerResource;
import tech.smartboot.feat.ai.mcp.server.model.ServerTool;
import tech.smartboot.feat.core.common.HeaderValue;
import tech.smartboot.feat.core.common.exception.FeatException;
import tech.smartboot.feat.core.server.HttpRequest;
import tech.smartboot.feat.router.Context;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.lang.reflect.Field;
import java.util.concurrent.CompletableFuture;

/**
 * @author 三刀 zhengjunweimail@163.com
 * @version v1.0.0
 */
public abstract class AbstractServiceLoader implements CloudService {
    protected static final byte[] b_success_false = {'"', 's', 'u', 'c', 'c', 'e', 's', 's', '"', ':', 'f', 'a', 'l', 's', 'e'};
    protected static final byte[] b_data = {'"', 'd', 'a', 't', 'a', '"', ':'};
    protected static final byte[] b_null = {'n', 'u', 'l', 'l'};
    protected static final byte[] b_code = {'"', 'c', 'o', 'd', 'e', '"', ':'};
    protected static final byte[] b_message = {'"', 'm', 'e', 's', 's', 'a', 'g', 'e', '"', ':'};
    protected static final byte[] b_success_true = {'"', 's', 'u', 'c', 'c', 'e', 's', 's', '"', ':', 't', 'r', 'u', 'e'};
    protected ThreadLocal<ByteArrayOutputStream> outputStream = ThreadLocal.withInitial(() -> new ByteArrayOutputStream(1024));

    protected JSONObject getParams(HttpRequest request) {
        try {
            if (request.getContentType() != null && request.getContentType().startsWith("application/json")) {
                ByteArrayOutputStream out = new ByteArrayOutputStream();
                byte[] bytes = new byte[1024];
                int len = 0;
                InputStream inputStream = request.getInputStream();
                while ((len = inputStream.read(bytes)) != -1) {
                    out.write(bytes, 0, len);
                }
                return JSON.parseObject(out.toByteArray());
            } else {
                JSONObject jsonObject = new JSONObject();
                request.getParameters().keySet().forEach(param -> {
                    jsonObject.put(param, request.getParameter(param));
                });
                return jsonObject;
            }
        } catch (Exception e) {
            throw new FeatException(e);
        }
    }

    protected void response(AsyncResponse response, Context ctx, CompletableFuture<Void> completableFuture) {
        gzipResponse(response, ctx, completableFuture, Integer.MAX_VALUE);
    }

    protected void gzipResponse(AsyncResponse response, Context ctx, CompletableFuture<Void> completableFuture, int threshold) {
        response.getFuture().thenAccept(result -> {
            if (result == null) {
                completableFuture.complete(null);
                return;
            }
            try {
                byte[] bytes = JSONObject.toJSONString(result).getBytes();
                ctx.Response.setContentType(HeaderValue.ContentType.APPLICATION_JSON);
                if (bytes.length > threshold) {
                    bytes = tech.smartboot.feat.core.common.FeatUtils.gzip(bytes);
                    ctx.Response.setHeader("Content-Encoding", "gzip");
                }
                ctx.Response.setContentLength(bytes.length);
                ctx.Response.write(bytes);
                completableFuture.complete(null);
            } catch (IOException e) {
                completableFuture.completeExceptionally(e);
            }
        }).exceptionally(throwable -> {
            completableFuture.completeExceptionally(throwable);
            return null;
        });

    }

    protected void writeLong(OutputStream out, long value) throws IOException {
//        writeInt(out, value);
        out.write(String.valueOf(value).getBytes());
    }

    protected void writeShort(OutputStream out, short value) throws IOException {
        writeInt(out, value);
    }

    private static final byte[] TRUE = new byte[]{'t', 'r', 'u', 'e'};
    private static final byte[] FALSE = new byte[]{'f', 'a', 'l', 's', 'e'};

    protected void writeBool(OutputStream out, boolean value) throws IOException {
        if (value) {
            out.write(TRUE);
        } else {
            out.write(FALSE);
        }
    }

    protected void writeByte(OutputStream out, byte value) throws IOException {
        if (value == 0) {
            out.write('0');
        } else if (value == Byte.MIN_VALUE) {
            out.write(new byte[]{'-', '1', '2', '8'});
        } else if (value < 0) {
            out.write('-');
            value = (byte) -value;
        }
        if (value < 10) {
            out.write('0' + value);
        } else if (value < 100) {
            out.write('0' + value / 10);
            out.write('0' + value % 10);
        } else {
            out.write('0' + value / 100);
            out.write('0' + value / 10 % 10);
            out.write('0' + value % 10);
        }
    }

    protected void writeInt(OutputStream out, int value) throws IOException {
        // 处理特殊情况 0
        if (value == 0) {
            out.write('0');
            return;
        } else if (value == Integer.MIN_VALUE) {
            out.write(new byte[]{'-', '2', '1', '4', '7', '4', '8', '3', '6', '4', '8'});
            return;
        } else if (value < 0) {
            out.write('-');
            value = -value;
        }

        if (value < 10) {
            out.write('0' + value);
        } else if (value < 100) {
            out.write('0' + value / 10);
            out.write('0' + value % 10);
        } else {
            // 用于存储转换后的数字字符
            byte[] buffer = new byte[10]; // 最大的 int 有 10 位
            int pos = 10;
            while (value != 0) {
                buffer[--pos] = (byte) ('0' + (value % 10));
                value /= 10;
            }
            out.write(buffer, pos, buffer.length - pos);
        }
    }

    protected void writeJsonValue(OutputStream os, String value) throws IOException {
        byte[] bytes = value.getBytes();
        int start = 0;
        for (int i = 0; i < bytes.length; i++) {
            byte b = bytes[i];
            if (b == '"') {
                os.write(bytes, start, i - start);
                os.write('\\');
                os.write('"');
                start = i + 1;
            } else if (b == '\n') {
                os.write(bytes, start, i - start);
                os.write('\\');
                os.write('n');
                start = i + 1;
            } else if (b == '\r') {
                os.write(bytes, start, i - start);
                os.write('\\');
                os.write('r');
                start = i + 1;
            }
        }
        if (start < bytes.length) {
            os.write(bytes, start, bytes.length - start);
        }
    }

    protected ByteArrayOutputStream getOutputStream() {
        ByteArrayOutputStream os = outputStream.get();
        if (os.size() > 1024) {
            os = new ByteArrayOutputStream(1024);
            outputStream.set(os);
        }
        os.reset();
        return os;
    }

    protected <T> T loadBean(String beanName, ApplicationContext context) {
        T result = context.getBean(beanName);
        if (result == null) {
            throw new FeatException("bean[" + beanName + "] not ready, please check it's @Bean.order");
        }
        return result;
    }

    protected void reflectAutowired(Object bean, String fieldName, Object val) throws NoSuchFieldException, IllegalAccessException {
        Field field = bean.getClass().getDeclaredField(fieldName);
        field.setAccessible(true);
        field.set(bean, val);
    }


    protected void printlnMcp(McpServer mcpServer) {
        McpOptions options = mcpServer.getOptions();
        String name = options.getImplementation().getName();
        String version = options.getImplementation().getVersion();
        String title = options.getImplementation().getTitle();

        String header = " MCP Server Information ";
        int borderLength = Math.max(header.length(), 40);
        StringBuilder borderBuilder = new StringBuilder(borderLength);
        for (int i = 0; i < borderLength; i++) {
            borderBuilder.append('=');
        }
        String border = borderBuilder.toString();

        System.out.println(border);
        System.out.println("\033[1;36m" + header + "\033[0m");
        System.out.println(border);

        // 服务器基本信息
        System.out.println("\033[1;33mServer Name    \033[0m : " + name);
        System.out.println("\033[1;33mServer Title   \033[0m : " + title);
        System.out.println("\033[1;33mServer Version \033[0m : " + version);
        System.out.println();

        // 端点信息
        System.out.println("\033[1;34mEndpoints:\033[0m");
        System.out.println("  MCP Endpoint          : " + options.getMcpEndpoint());
        System.out.println("  SSE Endpoint          : " + options.getSseEndpoint());
        System.out.println("  SSE Message Endpoint  : " + options.getSseMessageEndpoint());
        System.out.println();

        // 功能启用状态
        System.out.println("\033[1;34mFeatures:\033[0m");
        System.out.println("  Logging    : " + (options.isLoggingEnable() ? "\033[0;32mENABLED\033[0m" : "\033[0;31mDISABLED\033[0m"));
        System.out.println("  Prompts    : " + (options.isPromptsEnable() ? "\033[0;32mENABLED\033[0m" : "\033[0;31mDISABLED\033[0m"));
        System.out.println("  Resources  : " + (options.isResourceEnable() ? "\033[0;32mENABLED\033[0m" : "\033[0;31mDISABLED\033[0m"));
        System.out.println("  Tools      : " + (options.isToolEnable() ? "\033[0;32mENABLED\033[0m" : "\033[0;31mDISABLED\033[0m"));
        System.out.println();

        if (!mcpServer.getTools().isEmpty()) {
            System.out.println("\033[1;34mTools(" + mcpServer.getTools().size() + "):\033[0m");
            for (ServerTool tool : mcpServer.getTools()) {
                System.out.println(" \033[0;32m|->\033[0m " + tool.getName() + ": " + tool.getDescription());
            }
            System.out.println();
        }

        if (!mcpServer.getPrompts().isEmpty()) {
            System.out.println("\033[1;34mPrompts(" + mcpServer.getPrompts().size() + "):\033[0m");
            for (ServerPrompt prompt : mcpServer.getPrompts()) {
                System.out.println(" \033[0;32m|->\033[0m " + prompt.getName() + ": " + prompt.getDescription());
            }
            System.out.println();
        }

        if (!mcpServer.getResources().isEmpty()) {
            System.out.println("\033[1;34mResources(" + mcpServer.getResources().size() + "):\033[0m");
            for (ServerResource resource : mcpServer.getResources()) {
                System.out.println(" \033[0;32m|->\033[0m " + resource.getName() + ": " + resource.getDescription());
            }
            System.out.println();
        }

        if (!mcpServer.getResourceTemplates().isEmpty()) {
            System.out.println("\033[1;34mResource Templates(" + mcpServer.getResourceTemplates().size() + "):\033[0m");
            for (ResourceTemplate template : mcpServer.getResourceTemplates()) {
                System.out.println(" \033[0;32m|->\033[0m " + template.getName() + ": " + template.getDescription());
            }
            System.out.println();
        }

        System.out.println(border);
    }
}
