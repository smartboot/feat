/*******************************************************************************
 * Copyright (c) 2024, tech.smartboot. All rights reserved.
 * project name: feat
 * file name: HttpServerTest.java
 * Date: 2021-06-04
 * Author: sandao (zhengjunweimail@163.com)
 ******************************************************************************/

package tech.smartboot.feat.test.restful;

import org.junit.After;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.smartboot.socket.extension.plugins.StreamMonitorPlugin;
import tech.smartboot.feat.Feat;
import tech.smartboot.feat.core.client.HttpClient;
import tech.smartboot.feat.core.server.HttpServer;
import tech.smartboot.feat.test.BastTest;
import tech.smartboot.feat.test.server.RequestUnit;

import java.util.concurrent.ExecutionException;

/**
 * @author 三刀（zhengjunweimail@163.com）
 * @version V1.0 , 2021/6/4
 */
public class RestfulServerTest extends BastTest {
    public static final String KEY_PARAMETERS = "parameters";
    public static final String KEY_METHOD = "method";
    public static final String KEY_URI = "uri";
    public static final String KEY_URL = "url";
    public static final String KEY_HEADERS = "headers";
    private static final Logger LOGGER = LoggerFactory.getLogger(RestfulServerTest.class);
    private HttpServer bootstrap;
    private RequestUnit requestUnit;


    @Before
    public void init() throws Exception {
        bootstrap = Feat.cloudServer(opts -> {
            opts.setPackages(Demo1Controller.class.getName(), Demo2Controller.class.getName()).addPlugin(new StreamMonitorPlugin<>((asynchronousSocketChannel, bytes) -> System.out.println(new String(bytes)), (asynchronousSocketChannel, bytes) -> System.out.println(new String(bytes))));
        });
        bootstrap.listen(SERVER_PORT);
    }

    @Test
    public void testGet() throws ExecutionException, InterruptedException {
        HttpClient httpClient = getHttpClient();

        Assert.assertEquals(httpClient.get("/").submit().get().body(), "hello");
    }

    @Test
    public void testGet2() throws ExecutionException, InterruptedException {
        HttpClient httpClient = getHttpClient();
        Assert.assertEquals(httpClient.get("/demo2").submit().get().body(), "hello world");
    }


    @After
    public void destroy() {
        bootstrap.shutdown();
    }
}
