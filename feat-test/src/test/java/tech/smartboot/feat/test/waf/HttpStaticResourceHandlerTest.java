/*
 *  Copyright (C) [2024] smartboot [zhengjunweimail@163.com]
 *
 *  企业用户未经smartboot组织特别许可，需遵循Apache-2.0开源协议合理合法使用本项目。
 *
 *   Enterprise users are required to use this project reasonably
 *   and legally in accordance with the Apache-2.0 open source agreement
 *  without special permission from the smartboot organization.
 */

package tech.smartboot.feat.test.waf;

import org.junit.After;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import tech.smartboot.feat.core.client.HttpClient;
import tech.smartboot.feat.core.common.HttpStatus;
import tech.smartboot.feat.core.server.HttpServer;
import tech.smartboot.feat.core.server.handler.HttpStaticResourceHandler;
import tech.smartboot.feat.test.BastTest;

import java.io.File;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.util.concurrent.ExecutionException;

/**
 * 静态资源处理器路径穿越攻击防护测试
 *
 * @author 三刀 zhengjunweimail@163.com
 * @version v1.0.0
 */
public class HttpStaticResourceHandlerTest extends BastTest {
    private HttpServer bootstrap;
    private File tempDir;

    @Before
    public void init() throws IOException {
        //构造临时目录：baseDir/index.html 以及 baseDir 外部的 secret.txt
        tempDir = Files.createTempDirectory("feat-static").toFile();
        File baseDir = new File(tempDir, "base");
        Assert.assertTrue(baseDir.mkdirs());
        Files.write(new File(baseDir, "index.html").toPath(), "<html>index</html>".getBytes(StandardCharsets.UTF_8));
        Files.write(new File(tempDir, "secret.txt").toPath(), "secret".getBytes(StandardCharsets.UTF_8));

        bootstrap = new HttpServer();
        bootstrap.httpHandler(new HttpStaticResourceHandler(opt -> opt.baseDir(baseDir.getAbsolutePath())));
        bootstrap.options().debug(true);
        bootstrap.listen(SERVER_PORT);
    }

    @Test
    public void testPathTraversal() throws ExecutionException, InterruptedException {
        HttpClient httpClient = getHttpClient();
        //正常请求不受影响
        Assert.assertEquals(HttpStatus.OK.value(), httpClient.get("/index.html").submit().get().statusCode());

        //路径穿越：明文 ../
        Assert.assertEquals(HttpStatus.FORBIDDEN.value(), httpClient.get("/../secret.txt").submit().get().statusCode());
        //路径穿越：URL 编码 %2e%2e%2f，解码后为 ../
        Assert.assertEquals(HttpStatus.FORBIDDEN.value(), httpClient.get("/%2e%2e%2fsecret.txt").submit().get().statusCode());
        //路径穿越：部分 URL 编码 %2f，解码后为 ../
        Assert.assertEquals(HttpStatus.FORBIDDEN.value(), httpClient.get("/..%2fsecret.txt").submit().get().statusCode());

        //路径穿越：反斜杠 ..
        Assert.assertEquals(HttpStatus.FORBIDDEN.value(), httpClient.get("/..\\secret.txt").submit().get().statusCode());
        //路径穿越：URL 编码 %5c，解码后为 ..\
        Assert.assertEquals(HttpStatus.FORBIDDEN.value(), httpClient.get("/..%5csecret.txt").submit().get().statusCode());
    }

    @After
    public void destroy() {
        bootstrap.shutdown();
        tempDir.deleteOnExit();
    }
}
