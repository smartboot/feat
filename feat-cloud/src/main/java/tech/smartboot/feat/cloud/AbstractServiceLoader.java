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
import tech.smartboot.feat.core.common.exception.FeatException;
import tech.smartboot.feat.core.server.HttpRequest;
import tech.smartboot.feat.router.Context;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.concurrent.CompletableFuture;

/**
 * @author 三刀
 * @version v1.0.0
 */
public abstract class AbstractServiceLoader implements CloudService {
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

    protected void response(AsyncResponse response, Context ctx, CompletableFuture<Object> completableFuture) {
        response.getFuture().exceptionally(throwable -> {
            completableFuture.completeExceptionally(throwable);
            return null;
        }).thenAccept(result -> {
            if (result == null) {
                return;
            }
            try {
                byte[] bytes = JSONObject.toJSONString(result).getBytes();
                ctx.Response.setContentLength(bytes.length);
                ctx.Response.write(bytes);
                completableFuture.complete(result);
            } catch (IOException e) {
                completableFuture.completeExceptionally(e);
            }
        });

    }

    protected void writeJsonValue(OutputStream os, String value) throws IOException {
        byte[] bytes = value.getBytes();
        int start = 0;
        for (int i = 0; i < bytes.length; i++) {
            if (bytes[i] == '"') {
                os.write(bytes, start, i - start);
                os.write('\\');
                os.write('\"');
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
}
