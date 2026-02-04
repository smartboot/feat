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
import com.alibaba.fastjson2.JSONArray;
import com.alibaba.fastjson2.JSONObject;
import tech.smartboot.feat.ai.mcp.model.ResourceTemplate;
import tech.smartboot.feat.ai.mcp.server.McpOptions;
import tech.smartboot.feat.ai.mcp.server.McpServer;
import tech.smartboot.feat.ai.mcp.server.model.ServerPrompt;
import tech.smartboot.feat.ai.mcp.server.model.ServerResource;
import tech.smartboot.feat.ai.mcp.server.model.ServerTool;
import tech.smartboot.feat.core.common.FeatUtils;
import tech.smartboot.feat.core.common.HeaderValue;
import tech.smartboot.feat.core.common.HttpStatus;
import tech.smartboot.feat.core.common.exception.FeatException;
import tech.smartboot.feat.core.common.exception.HttpException;
import tech.smartboot.feat.core.server.HttpRequest;
import tech.smartboot.feat.core.server.impl.HttpEndpoint;
import tech.smartboot.feat.router.Context;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.lang.reflect.Field;
import java.util.Calendar;
import java.util.Date;
import java.util.concurrent.CompletableFuture;

/**
 * 抽象云服务类，提供云服务的基础实现和工具方法
 * <p>
 * 该类是所有云服务实现的基类，提供了诸如异步请求处理、JSON参数解析、
 * 响应结果封装、数据类型序列化等通用功能。
 * </p>
 *
 * @author 三刀 zhengjunweimail@163.com
 * @version v1.0.0
 */
public abstract class AbstractCloudService implements CloudService {
    /**
     * JSON响应中表示success=false的字节序列
     */
    protected static final byte[] b_success_false = {',', '"', 's', 'u', 'c', 'c', 'e', 's', 's', '"', ':', 'f', 'a', 'l', 's', 'e'};

    /**
     * JSON响应中"data"字段的字节序列
     */
    protected static final byte[] b_data = {',', '"', 'd', 'a', 't', 'a', '"', ':'};

    /**
     * JSON中的null值字节序列
     */
    protected static final byte[] b_null = {'n', 'u', 'l', 'l'};

    /**
     * JSON响应中"code"字段的字节序列
     */
    protected static final byte[] b_code = {',', '"', 'c', 'o', 'd', 'e', '"', ':'};

    /**
     * JSON响应中"message"字段的字节序列
     */
    protected static final byte[] b_message = {',', '"', 'm', 'e', 's', 's', 'a', 'g', 'e', '"', ':'};

    /**
     * JSON响应中表示success=true的字节序列
     */
    protected static final byte[] b_success_true = {',', '"', 's', 'u', 'c', 'c', 'e', 's', 's', '"', ':', 't', 'r', 'u', 'e'};

    /**
     * 空JSON对象的字节序列
     */
    protected static final byte[] b_empty_map = {'{', '}'};

    /**
     * 空JSON数组的字节序列
     */
    protected static final byte[] b_empty_list = {'[', ']'};

    /**
     * 布尔值true的字节序列
     */
    private static final byte[] TRUE = new byte[]{'t', 'r', 'u', 'e'};

    /**
     * 布尔值false的字节序列
     */
    private static final byte[] FALSE = new byte[]{'f', 'a', 'l', 's', 'e'};

    /**
     * 线程本地变量，用于提供可重用的字节输出流，避免频繁创建对象
     */
    protected static final ThreadLocal<ByteArrayOutputStream> outputStream = ThreadLocal.withInitial(() -> new ByteArrayOutputStream(1024));

    /**
     * 线程本地变量，用于提供可重用的日期格式化字节数组
     */
    protected static final ThreadLocal<byte[]> dateFormat = ThreadLocal.withInitial(() -> new byte[]{'"', '0', '0', '0', '0', '-', '0', '0', '-', '0', '0', ' ', '0', '0', ':', '0', '0', ':', '0', '0', '"'});

    /**
     * 设置异步读取请求体的升级处理器
     * <p>
     * 当请求体较大或需要异步处理时，将当前请求升级为异步读取模式
     * </p>
     *
     * @param request HTTP端点请求对象
     */
    protected void bodyAsyncRead(HttpEndpoint request) {
        // 获取请求体长度
        long contentLength = request.getContentLength();
        // 如果没有内容长度则直接返回
        if (contentLength < 0) {
            return;
        }
        // 如果内容长度超过最大请求大小限制，则抛出异常
        if (contentLength > request.getOptions().getMaxRequestSize()) {
            throw new HttpException(HttpStatus.PAYLOAD_TOO_LARGE);
        }
        // 获取内容类型
        String contentType = request.getContentType();
        // 如果内容类型为空或者不是表单或JSON类型，则直接返回
        if (contentType == null || !(contentType.startsWith(HeaderValue.ContentType.X_WWW_FORM_URLENCODED) || contentType.startsWith(HeaderValue.ContentType.APPLICATION_JSON))) {
            return;
        }
        // 设置升级处理器，用于异步读取请求体
        request.setUpgrade(new AsyncBodyReadUpgrade(request, (int) contentLength));
    }

    protected JSONArray toJsonArray(HttpRequest request) {
        try {
            if (request.getContentType() != null && request.getContentType().startsWith(HeaderValue.ContentType.APPLICATION_JSON)) {
                // 创建字节输出流用于存储请求体数据
                ByteArrayOutputStream out = new ByteArrayOutputStream();
                byte[] bytes = new byte[1024];
                int len = 0;
                // 获取输入流
                InputStream inputStream = request.getInputStream();
                // 循环读取请求体数据
                while ((len = inputStream.read(bytes)) != -1) {
                    out.write(bytes, 0, len);
                }
                // 将字节数组解析为JSONObject
                return JSON.parseArray(out.toByteArray());
            } else {
                throw new HttpException(HttpStatus.INTERNAL_SERVER_ERROR);
            }
        } catch (Throwable e) {
            throw new HttpException(HttpStatus.INTERNAL_SERVER_ERROR);
        }
    }

    /**
     * 从HTTP请求中获取参数并转换为JSONObject
     * <p>
     * 支持JSON格式和表单格式的请求参数解析
     * </p>
     *
     * @param request HTTP请求对象
     * @return 解析后的参数JSONObject
     * @throws FeatException 当参数解析失败时抛出异常
     */
    protected JSONObject getParams(HttpRequest request) {
        try {
            // 如果内容类型是JSON格式
            if (request.getContentType() != null && request.getContentType().startsWith(HeaderValue.ContentType.APPLICATION_JSON)) {
                // 创建字节输出流用于存储请求体数据
                ByteArrayOutputStream out = new ByteArrayOutputStream();
                byte[] bytes = new byte[1024];
                int len = 0;
                // 获取输入流
                InputStream inputStream = request.getInputStream();
                // 循环读取请求体数据
                while ((len = inputStream.read(bytes)) != -1) {
                    out.write(bytes, 0, len);
                }
                // 将字节数组解析为JSONObject
                JSONObject jsonObject = JSON.parseObject(out.toByteArray());
                if (jsonObject == null) {
                    throw new FeatException("invalid body for application/json request ");
                }
                return jsonObject;
            } else {
                // 创建JSONObject用于存储参数
                JSONObject jsonObject = new JSONObject();
                // 遍历所有请求参数并放入JSONObject中
                request.getParameters().keySet().forEach(param -> {
                    jsonObject.put(param, request.getParameter(param));
                });
                return jsonObject;
            }
        } catch (FeatException e) {
            throw e;
        } catch (Exception e) {
            // 如果解析过程中出现异常，则抛出FeatException
            throw new FeatException(e);
        }
    }

    /**
     * 处理异步响应结果
     * <p>
     * 将响应结果转换为JSON格式并写入HTTP响应
     * </p>
     *
     * @param response          异步响应对象
     * @param ctx               请求上下文
     * @param completableFuture 用于完成异步操作的CompletableFuture
     */
    protected void response(AsyncResponse response, Context ctx, CompletableFuture<Void> completableFuture) {
        // 调用gzipResponse方法处理响应，阈值设为最大整数（即不启用gzip压缩）
        gzipResponse(response, ctx, completableFuture, Integer.MAX_VALUE);
    }

    /**
     * 处理并发送gzip压缩的响应结果
     * <p>
     * 将响应结果转换为JSON格式，根据数据大小决定是否进行gzip压缩后写入HTTP响应
     * </p>
     *
     * @param response          异步响应对象
     * @param ctx               请求上下文
     * @param completableFuture 用于完成异步操作的CompletableFuture
     * @param threshold         启用gzip压缩的阈值，当响应数据大小超过此值时启用压缩
     */
    protected void gzipResponse(AsyncResponse response, Context ctx, CompletableFuture<Void> completableFuture, int threshold) {
        // 当异步响应完成时执行以下操作
        response.getFuture().thenAccept(result -> {
            // 如果响应结果为空，则完成异步操作
            if (result == null) {
                completableFuture.complete(null);
                return;
            }
            try {
                // 将响应结果转换为JSON字符串并获取字节数组
                byte[] bytes = JSONObject.toJSONString(result).getBytes();
                // 设置响应内容类型为JSON
                ctx.Response.setContentType(HeaderValue.ContentType.APPLICATION_JSON);
                // 如果数据大小超过阈值，则进行gzip压缩
                if (bytes.length > threshold) {
                    bytes = FeatUtils.gzip(bytes);
                    ctx.Response.setHeader("Content-Encoding", "gzip");
                }
                // 设置响应内容长度并写入响应数据
                ctx.Response.setContentLength(bytes.length);
                ctx.Response.write(bytes);
                // 完成异步操作
                completableFuture.complete(null);
            } catch (IOException e) {
                // 如果出现IO异常，则以异常完成异步操作
                completableFuture.completeExceptionally(e);
            }
        }).exceptionally(throwable -> {
            // 如果在处理过程中出现异常，则以异常完成异步操作
            completableFuture.completeExceptionally(throwable);
            return null;
        });

    }

    /**
     * 将长整型数值写入输出流
     *
     * @param out   输出流
     * @param value 要写入的长整型值
     * @throws IOException 当IO操作出现异常时抛出
     */
    protected void writeLong(OutputStream out, long value) throws IOException {
//        writeInt(out, value);
        // 将长整型值转换为字符串并写入输出流
        out.write(String.valueOf(value).getBytes());
    }

    /**
     * 将短整型数值写入输出流
     *
     * @param out   输出流
     * @param value 要写入的短整型值
     * @throws IOException 当IO操作出现异常时抛出
     */
    protected void writeShort(OutputStream out, short value) throws IOException {
        // 复用writeInt方法处理短整型值
        writeInt(out, value);
    }


    /**
     * 将布尔值写入输出流
     *
     * @param out   输出流
     * @param value 要写入的布尔值
     * @throws IOException 当IO操作出现异常时抛出
     */
    protected void writeBool(OutputStream out, boolean value) throws IOException {
        // 根据布尔值写入对应的字节序列
        if (value) {
            out.write(TRUE);
        } else {
            out.write(FALSE);
        }
    }

    /**
     * 将双精度浮点数写入输出流
     *
     * @param out   输出流
     * @param value 要写入的双精度浮点数
     * @throws IOException 当IO操作出现异常时抛出
     */
    protected void writeNumber(OutputStream out, double value) throws IOException {
        // 将双精度浮点数转换为字符串并写入输出流
        out.write(String.valueOf(value).getBytes());
    }

    /**
     * 将字符写入输出流，并进行JSON转义处理
     *
     * @param out   输出流
     * @param value 要写入的字符
     * @throws IOException 当IO操作出现异常时抛出
     */
    protected void writeChar(OutputStream out, char value) throws IOException {
        // 写入开始引号
        out.write('"');
        // 根据字符类型进行转义处理
        if (value == '"') {
            // 双引号需要转义
            out.write('\\');
            out.write('"');
        } else if (value >= 32 && value < 127) {
            // 普通ASCII字符直接写入
            out.write(value);
        } else {
            // 其他字符使用Unicode转义
            out.write(String.format("\\u%04x", (int) value).getBytes());
        }
        // 写入结束引号
        out.write('"');
    }

    /**
     * 将单精度浮点数写入输出流
     *
     * @param out   输出流
     * @param value 要写入的单精度浮点数
     * @throws IOException 当IO操作出现异常时抛出
     */
    protected void writeNumber(OutputStream out, float value) throws IOException {
        // 将单精度浮点数转换为字符串并写入输出流
        out.write(String.valueOf(value).getBytes());
    }

    /**
     * 将字节值写入输出流
     *
     * @param out   输出流
     * @param value 要写入的字节值
     * @throws IOException 当IO操作出现异常时抛出
     */
    protected void writeByte(OutputStream out, byte value) throws IOException {
        // 特殊值处理
        if (value == 0) {
            out.write('0');
        } else if (value == Byte.MIN_VALUE) {
            // 字节最小值特殊处理
            out.write(new byte[]{'-', '1', '2', '8'});
        } else if (value < 0) {
            // 负数处理
            out.write('-');
            value = (byte) -value;
        }

        // 根据数值大小进行不同的写入方式
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

    /**
     * 将日期值写入输出流，格式为"yyyy-MM-dd HH:mm:ss"
     *
     * @param out   输出流
     * @param value 要写入的日期值
     * @throws IOException 当IO操作出现异常时抛出
     */
    protected void writeDate(OutputStream out, Date value) throws IOException {
        // 获取日期格式化字节数组
        byte[] bytes = dateFormat.get();
        // 使用日历对象处理日期
        Calendar calendar = Calendar.getInstance();
        calendar.setTime(value);

        // 提取年份并写入字节数组
        int v = calendar.get(Calendar.YEAR);
        bytes[1] = (byte) (v / 1000 + '0');
        bytes[2] = (byte) (v / 100 % 10 + '0');
        bytes[3] = (byte) (v / 10 % 10 + '0');
        bytes[4] = (byte) (v % 10 + '0');

        // 提取月份并写入字节数组（月份需要+1）
        v = calendar.get(Calendar.MONTH) + 1;
        bytes[6] = (byte) (v / 10 + '0');
        bytes[7] = (byte) (v % 10 + '0');

        // 提取日期并写入字节数组
        v = calendar.get(Calendar.DAY_OF_MONTH);
        bytes[9] = (byte) (v / 10 + '0');
        bytes[10] = (byte) (v % 10 + '0');

        // 提取小时并写入字节数组
        v = calendar.get(Calendar.HOUR_OF_DAY);
        bytes[12] = (byte) (v / 10 + '0');
        bytes[13] = (byte) (v % 10 + '0');

        // 提取分钟并写入字节数组
        v = calendar.get(Calendar.MINUTE);
        bytes[15] = (byte) (v / 10 + '0');
        bytes[16] = (byte) (v % 10 + '0');

        // 提取秒并写入字节数组
        v = calendar.get(Calendar.SECOND);
        bytes[18] = (byte) (v / 10 + '0');
        bytes[19] = (byte) (v % 10 + '0');
        // 将格式化后的字节数组写入输出流
        out.write(bytes);
    }

    /**
     * 将整数值写入输出流
     *
     * @param out   输出流
     * @param value 要写入的整数值
     * @throws IOException 当IO操作出现异常时抛出
     */
    protected void writeInt(OutputStream out, int value) throws IOException {
        // 处理特殊情况 0
        if (value == 0) {
            out.write('0');
            return;
        } else if (value == Integer.MIN_VALUE) {
            // 整数最小值特殊处理
            out.write(new byte[]{'-', '2', '1', '4', '7', '4', '8', '3', '6', '4', '8'});
            return;
        } else if (value < 0) {
            // 负数处理
            out.write('-');
            value = -value;
        }

        // 根据数值大小进行不同的写入方式
        if (value < 10) {
            out.write('0' + value);
        } else if (value < 100) {
            out.write('0' + value / 10);
            out.write('0' + value % 10);
        } else if (value < 1000) {
            out.write('0' + value / 100);
            out.write('0' + value / 10 % 10);
            out.write('0' + value % 10);
        } else {
            // 用于存储转换后的数字字符
            byte[] buffer = new byte[10]; // 最大的 int 有 10 位
            int pos = 10;
            // 逐位转换数字为字符
            while (value != 0) {
                buffer[--pos] = (byte) ('0' + (value % 10));
                value /= 10;
            }
            // 写入转换后的字节序列
            out.write(buffer, pos, buffer.length - pos);
        }
    }

    /**
     * 将字符串值写入输出流，并进行JSON特殊字符转义处理
     *
     * @param os    输出流
     * @param value 要写入的字符串值
     * @throws IOException 当IO操作出现异常时抛出
     */
    protected void writeJsonValue(OutputStream os, String value) throws IOException {
        // 获取字符串的字节数组
        byte[] bytes = value.getBytes();
        int start = 0;

        // 使用查表法处理特殊字符转义，提高代码简洁性
        for (int i = 0; i < bytes.length; i++) {
            byte b = bytes[i];
            byte escapeChar = 0;

            // 根据字符类型确定转义字符
            switch (b) {
                case '"':
                    escapeChar = '"';
                    break;
                case '\n':
                    escapeChar = 'n';
                    break;
                case '\r':
                    escapeChar = 'r';
                    break;
                case '\\':
                    escapeChar = '\\';
                    break;
                default:
                    continue; // 非特殊字符，继续循环
            }

            // 统一处理转义逻辑
            os.write(bytes, start, i - start);
            os.write('\\');
            os.write(escapeChar);
            start = i + 1;
        }

        // 写入剩余的字节数据
        if (start < bytes.length) {
            os.write(bytes, start, bytes.length - start);
        }
    }

    /**
     * 获取可重用的字节输出流
     * <p>
     * 使用ThreadLocal确保线程安全，并在输出流过大时重新创建以避免内存浪费
     * </p>
     *
     * @return 可用的字节输出流
     */
    protected ByteArrayOutputStream getOutputStream() {
        // 从ThreadLocal中获取输出流
        ByteArrayOutputStream os = outputStream.get();
        // 如果输出流过大，则重新创建一个
        if (os.size() > 1024) {
            os = new ByteArrayOutputStream(1024);
            outputStream.set(os);
        }
        // 重置输出流并返回
        os.reset();
        return os;
    }

    /**
     * 从应用上下文中加载指定名称的Bean
     *
     * @param beanName Bean名称
     * @param context  应用上下文
     * @param <T>      Bean类型
     * @return 指定名称的Bean实例
     * @throws FeatException 当Bean不存在时抛出异常
     */
    protected <T> T loadBean(String beanName, ApplicationContext context) {
        // 从上下文中获取Bean
        T result = context.getBean(beanName);
        // 如果Bean不存在，则抛出异常
        if (result == null) {
            throw new FeatException("bean[" + beanName + "] not ready, please check it's @Bean.order");
        }
        return result;
    }

    /**
     * 通过反射机制为对象的指定字段注入值
     *
     * @param bean      目标对象
     * @param fieldName 字段名称
     * @param val       要注入的值
     * @throws NoSuchFieldException   当字段不存在时抛出异常
     * @throws IllegalAccessException 当字段不可访问时抛出异常
     */
    protected void reflectAutowired(Object bean, String fieldName, Object val) throws NoSuchFieldException, IllegalAccessException {
        // 获取指定字段
        Field field = bean.getClass().getDeclaredField(fieldName);
        // 设置字段可访问
        field.setAccessible(true);
        // 为字段设置值
        field.set(bean, val);
    }


    /**
     * 打印MCP服务器信息
     * <p>
     * 以格式化的方式在控制台输出MCP服务器的相关信息，包括基本信息、端点信息、功能状态和资源列表等
     * </p>
     *
     * @param mcpServer MCP服务器实例
     */
    protected void printlnMcp(McpServer mcpServer) {
        // 获取MCP选项配置
        McpOptions options = mcpServer.getOptions();
        // 获取服务器实现信息
        String name = options.getImplementation().getName();
        String version = options.getImplementation().getVersion();
        String title = options.getImplementation().getTitle();

        // 构建标题和边框
        String header = " MCP Server Information ";
        int borderLength = Math.max(header.length(), 40);
        StringBuilder borderBuilder = new StringBuilder(borderLength);
        for (int i = 0; i < borderLength; i++) {
            borderBuilder.append('=');
        }
        String border = borderBuilder.toString();

        // 打印服务器信息标题
        System.out.println(border);
        System.out.println("\033[1;36m" + header + "\033[0m");
        System.out.println(border);

        // 打印服务器基本信息
        System.out.println("\033[1;33mServer Name    \033[0m : " + name);
        System.out.println("\033[1;33mServer Title   \033[0m : " + title);
        System.out.println("\033[1;33mServer Version \033[0m : " + version);
        System.out.println();

        // 打印端点信息
        System.out.println("\033[1;34mEndpoints:\033[0m");
        System.out.println("  MCP Endpoint          : " + options.getMcpEndpoint());
        System.out.println("  SSE Endpoint          : " + options.getSseEndpoint());
        System.out.println("  SSE Message Endpoint  : " + options.getSseMessageEndpoint());
        System.out.println();

        // 打印功能启用状态
        System.out.println("\033[1;34mFeatures:\033[0m");
        System.out.println("  Logging    : " + (options.isLoggingEnable() ? "\033[0;32mENABLED\033[0m" : "\033[0;31mDISABLED\033[0m"));
        System.out.println("  Prompts    : " + (options.isPromptsEnable() ? "\033[0;32mENABLED\033[0m" : "\033[0;31mDISABLED\033[0m"));
        System.out.println("  Resources  : " + (options.isResourceEnable() ? "\033[0;32mENABLED\033[0m" : "\033[0;31mDISABLED\033[0m"));
        System.out.println("  Tools      : " + (options.isToolEnable() ? "\033[0;32mENABLED\033[0m" : "\033[0;31mDISABLED\033[0m"));
        System.out.println();

        // 打印工具列表
        if (!mcpServer.getTools().isEmpty()) {
            System.out.println("\033[1;34mTools(" + mcpServer.getTools().size() + "):\033[0m");
            for (ServerTool tool : mcpServer.getTools()) {
                System.out.println(" \033[0;32m|->\033[0m " + tool.getName() + ": " + tool.getDescription());
            }
            System.out.println();
        }

        // 打印提示信息列表
        if (!mcpServer.getPrompts().isEmpty()) {
            System.out.println("\033[1;34mPrompts(" + mcpServer.getPrompts().size() + "):\033[0m");
            for (ServerPrompt prompt : mcpServer.getPrompts()) {
                System.out.println(" \033[0;32m|->\033[0m " + prompt.getName() + ": " + prompt.getDescription());
            }
            System.out.println();
        }

        // 打印资源列表
        if (!mcpServer.getResources().isEmpty()) {
            System.out.println("\033[1;34mResources(" + mcpServer.getResources().size() + "):\033[0m");
            for (ServerResource resource : mcpServer.getResources()) {
                System.out.println(" \033[0;32m|->\033[0m " + resource.getName() + ": " + resource.getDescription());
            }
            System.out.println();
        }

        // 打印资源模板列表
        if (!mcpServer.getResourceTemplates().isEmpty()) {
            System.out.println("\033[1;34mResource Templates(" + mcpServer.getResourceTemplates().size() + "):\033[0m");
            for (ResourceTemplate template : mcpServer.getResourceTemplates()) {
                System.out.println(" \033[0;32m|->\033[0m " + template.getName() + ": " + template.getDescription());
            }
            System.out.println();
        }

        // 打印结束边框
        System.out.println(border);
    }

    /**
     * 判断服务类是否在指定的包范围内
     * <p>
     * 用于控制哪些服务类可以被加载和处理
     * </p>
     *
     * @param context   应用上下文
     * @param clazzName 类名
     * @return 如果服务类在指定包范围内返回true，否则返回false
     */
    protected final boolean acceptService(ApplicationContext context, String clazzName) {
        // 如果配置了包范围，则检查类名是否以指定包名开头
        if (context.getOptions().getPackages() != null && context.getOptions().getPackages().length > 0) {
            for (String pkg : context.getOptions().getPackages()) {
                if (clazzName.startsWith(pkg)) {
                    return true;
                }
            }
            return false;
        }
        // 如果未配置包范围，则接受所有服务类
        return true;
    }

    /**
     * 打印路由映射信息
     * <p>
     * 在控制台以格式化的方式输出路由与控制器方法的映射关系
     * </p>
     *
     * @param router     路由路径
     * @param controller 控制器类名
     * @param method     控制器方法名
     */
    protected static void printRouter(String router, String controller, String method) {
        // 使用绿色输出路由映射信息
        System.out.println(" \u001B[32m|->\u001B[0m " + router + " ==> " + controller + "@" + method);
    }
}