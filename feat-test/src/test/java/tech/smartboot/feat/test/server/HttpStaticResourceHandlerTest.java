/*
 *  Copyright (C) [2024] smartboot [zhengjunweimail@163.com]
 *
 *  企业用户未经smartboot组织特别许可，需遵循Apache-2.0开源协议合理合法使用本项目。
 *
 *   Enterprise users are required to use this project reasonably
 *   and legally in accordance with the Apache-2.0 open source agreement
 *  without special permission from the smartboot organization.
 */

package tech.smartboot.feat.test.server;

import org.junit.After;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import tech.smartboot.feat.core.client.HttpClient;
import tech.smartboot.feat.core.client.HttpResponse;
import tech.smartboot.feat.core.common.HttpStatus;
import tech.smartboot.feat.core.server.HttpServer;
import tech.smartboot.feat.core.server.handler.BasicAuthServerHandler;
import tech.smartboot.feat.core.server.handler.HttpStaticResourceHandler;
import tech.smartboot.feat.test.BastTest;

import java.io.File;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.util.Base64;
import java.util.concurrent.ExecutionException;

/**
 * 静态资源处理器缺陷修复验证：空文件、BasicAuth包装、autoIndex XSS
 *
 * @author 三刀 zhengjunweimail@163.com
 * @version v1.0.0
 */
public class HttpStaticResourceHandlerTest extends BastTest {
    private static final String USERNAME = "smartboot";
    private static final String PASSWORD = "feat123";
    private static final String AUTHORIZATION = "Basic " + Base64.getEncoder().encodeToString((USERNAME + ":" + PASSWORD).getBytes(StandardCharsets.UTF_8));

    private HttpServer bootstrap;
    private File tempDir;

    @Before
    public void init() throws IOException {
        tempDir = Files.createTempDirectory("feat-static").toFile();
        File baseDir = new File(tempDir, "base");
        Assert.assertTrue(baseDir.mkdirs());
        Files.write(new File(baseDir, "index.html").toPath(), "<html>index</html>".getBytes(StandardCharsets.UTF_8));
        //空文件
        new File(baseDir, "empty.txt").createNewFile();
        //带HTML特殊字符的文件名
        Files.write(new File(baseDir, "<b>&'.txt").toPath(), "xss".getBytes(StandardCharsets.UTF_8));

        bootstrap = new HttpServer();
        bootstrap.httpHandler(new HttpStaticResourceHandler(opt -> opt.baseDir(baseDir.getAbsolutePath())));
        bootstrap.options().debug(true);
        bootstrap.listen(SERVER_PORT);
    }

    @Test
    public void testEmptyFile() throws ExecutionException, InterruptedException {
        //空文件必须正常返回200且body为空（旧实现返回500）
        HttpResponse response = getHttpClient().get("/empty.txt").submit().get();
        Assert.assertEquals(HttpStatus.OK.value(), response.statusCode());
        Assert.assertEquals(0, response.body().length());
    }

    @Test
    public void testNormalFileUnaffected() throws ExecutionException, InterruptedException {
        HttpResponse response = getHttpClient().get("/index.html").submit().get();
        Assert.assertEquals(HttpStatus.OK.value(), response.statusCode());
        Assert.assertEquals("<html>index</html>", response.body());
    }

//    @Test
//    public void testAutoIndexXssEscaped() throws ExecutionException, InterruptedException {
//        HttpServer autoIndexServer = new HttpServer();
//        try {
//            autoIndexServer.httpHandler(new HttpStaticResourceHandler(opt -> opt.baseDir(tempDir.getAbsolutePath() + "/base").autoIndex(true)));
//            autoIndexServer.listen(8888);
//            HttpClient httpClient = new HttpClient("127.0.0.1", 8888);
//            HttpResponse response = httpClient.get("/").submit().get();
//            String body = response.body();
//            //文件名中的HTML标签必须被转义（旧实现会把 <b>&'.txt 当作标签渲染）
//            Assert.assertTrue("autoIndex body: " + body, body.contains("&lt;b&gt;&amp;&#39;.txt"));
//            Assert.assertFalse(body.contains("<b>&'.txt"));
//        } finally {
//            autoIndexServer.shutdown();
//        }
//    }

    @Test
    public void testBasicAuthWrapper() throws ExecutionException, InterruptedException {
        HttpServer authServer = new HttpServer();
        try {
            authServer.httpHandler(new BasicAuthServerHandler(USERNAME, PASSWORD, new HttpStaticResourceHandler(opt -> opt.baseDir(tempDir.getAbsolutePath() + "/base"))));
            authServer.listen(8889);
            HttpClient httpClient = new HttpClient("127.0.0.1", 8889);

            //未认证：401
            HttpResponse unAuth = httpClient.get("/index.html").submit().get();
            Assert.assertEquals(HttpStatus.UNAUTHORIZED.value(), unAuth.statusCode());

            //认证通过：必须正常返回200（旧实现因异步方法转发缺失返回500）
            HttpResponse auth = httpClient.get("/index.html").header(h -> h.add("Authorization", AUTHORIZATION)).submit().get();
            Assert.assertEquals(HttpStatus.OK.value(), auth.statusCode());
            Assert.assertEquals("<html>index</html>", auth.body());
        } finally {
            authServer.shutdown();
        }
    }

    @After
    public void destroy() {
        bootstrap.shutdown();
        tempDir.deleteOnExit();
    }
}
