package tech.smartboot.feat.demo.plus.common;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.concurrent.CompletableFuture;

import tech.smartboot.feat.core.server.HttpHandler;
import tech.smartboot.feat.core.server.HttpRequest;
import tech.smartboot.feat.core.server.HttpResponse;
import tech.smartboot.feat.core.server.handler.HttpStaticResourceHandler;

/**
 * 静态资源 + SPA 路由回退。
 * <p>
 * 前端的 /dashboard、/users 这些地址由浏览器端路由处理，
 * 直接刷新时服务端并没有对应文件，默认的静态资源处理器会去试 users.html 然后返回 404。
 * 这里按"有无扩展名"分流：有扩展名的当静态资源，其余一律返回 index.html，
 * 交回给前端路由；/api 下未匹配的路径仍然返回 JSON 404，避免把接口错误伪装成页面。
 * <p>
 * 注意：必须走异步 handle 方法。HttpStaticResourceHandler 的同步 handle 会直接抛
 * UnsupportedOperationException，只能在异步链路里被调用。
 */
public class SpaFallbackHandler implements HttpHandler {

    private static final byte[] API_NOT_FOUND =
            "{\"success\":false,\"code\":404,\"message\":\"接口不存在\",\"data\":null}".getBytes(StandardCharsets.UTF_8);

    /**
     * 前端产物未构建时的占位页。宁可给一页说明，也不要让服务启动失败，
     * 否则只有 API 需求的场景会被前端构建阻塞。
     */
    private static final byte[] MISSING_FRONTEND = ("<!doctype html><html lang=\"zh-CN\"><head>"
            + "<meta charset=\"UTF-8\"><title>Feat Plus</title>"
            + "<style>body{font-family:system-ui,sans-serif;max-width:640px;margin:80px auto;padding:0 24px;"
            + "line-height:1.7;color:#222}code{background:#f4f4f5;padding:2px 6px;border-radius:4px}</style></head><body>"
            + "<h2>前端产物尚未构建</h2>"
            + "<p>后端 API 已可用，但 <code>src/main/resources/static</code> 里没有页面文件。</p>"
            + "<p>在 <code>demo/feat-plus</code> 下执行：</p>"
            + "<pre><code>cd web &amp;&amp; yarn install &amp;&amp; yarn build\n"
            + "cp -r web/dist/. src/main/resources/static/</code></pre>"
            + "<p>然后重新执行 <code>mvn compile</code> 并重启服务。</p>"
            + "</body></html>").getBytes(StandardCharsets.UTF_8);

    private final HttpStaticResourceHandler staticHandler;
    private final byte[] indexHtml;

    public SpaFallbackHandler() throws IOException {
        this.staticHandler = new HttpStaticResourceHandler(options -> options.baseDir("classpath:static"));
        this.indexHtml = readIndexHtml();
    }

    @Override
    public void handle(HttpRequest request) throws Throwable {
        handle(request, new CompletableFuture<Void>());
    }

    @Override
    public void handle(HttpRequest request, CompletableFuture<Void> future) throws Throwable {
        String uri = request.getRequestURI();

        if (hasExtension(uri)) {
            staticHandler.handle(request, future);
            return;
        }

        HttpResponse response = request.getResponse();
        if (uri.startsWith("/api/")) {
            response.setHttpStatus(404, "Not Found");
            response.setContentType("application/json; charset=utf-8");
            response.setContentLength(API_NOT_FOUND.length);
            response.write(API_NOT_FOUND);
        } else {
            response.setContentType("text/html; charset=utf-8");
            response.setContentLength(indexHtml.length);
            response.write(indexHtml);
        }
        future.complete(null);
    }

    private static boolean hasExtension(String uri) {
        int lastSlash = uri.lastIndexOf('/');
        return uri.lastIndexOf('.') > lastSlash;
    }

    private static byte[] readIndexHtml() {
        InputStream in = Thread.currentThread().getContextClassLoader()
                .getResourceAsStream("static/index.html");
        if (in == null) {
            return MISSING_FRONTEND;
        }
        try {
            ByteArrayOutputStream out = new ByteArrayOutputStream();
            byte[] buffer = new byte[4096];
            int read;
            while ((read = in.read(buffer)) != -1) {
                out.write(buffer, 0, read);
            }
            return out.toByteArray();
        } catch (IOException e) {
            return MISSING_FRONTEND;
        } finally {
            try {
                in.close();
            } catch (IOException ignored) {
                // 读取完成，关闭失败不影响响应
            }
        }
    }
}
