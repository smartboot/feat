/*
 *  Copyright (C) [2024] smartboot [zhengjunweimail@163.com]
 *
 *  企业用户未经smartboot组织特别许可，需遵循AGPL-3.0开源协议合理合法使用本项目。
 *
 *   Enterprise users are required to use this project reasonably
 *   and legally in accordance with the AGPL-3.0 open source agreement
 *  without special permission from the smartboot organization.
 */

package tech.smartboot.feat.test.restful;

import org.junit.After;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.smartboot.socket.extension.plugins.StreamMonitorPlugin;
import tech.smartboot.feat.cloud.FeatCloud;
import tech.smartboot.feat.cloud.annotation.Controller;
import tech.smartboot.feat.cloud.annotation.RequestMapping;
import tech.smartboot.feat.cloud.annotation.Value;
import tech.smartboot.feat.core.client.HttpClient;
import tech.smartboot.feat.core.server.HttpServer;
import tech.smartboot.feat.test.BastTest;

/**
 * @author 三刀
 * @version v1.0 5/24/25
 */
@Controller("values")
public class ValueControllerTest extends BastTest {
    private HttpServer bootstrap;

    @Value
    private int param1;

    @Value
    private int[] param2;

    @RequestMapping("/param1")
    public int param1() {
        return param1;
    }

    @Before
    public void init() throws Exception {
        bootstrap = FeatCloud.cloudServer(opts -> {
            opts.setPackages("tech.smartboot.feat.test.restful").addPlugin(new StreamMonitorPlugin<>((asynchronousSocketChannel, bytes) -> System.out.println(new String(bytes)),
                    (asynchronousSocketChannel, bytes) -> System.out.println(new String(bytes))));
        });
        bootstrap.listen(SERVER_PORT);
    }

    @Test
    public void testParam1() throws Exception {
        HttpClient httpClient = getHttpClient();
        Assert.assertEquals(httpClient.post("/values/param1").submit().get().body(), "100");
    }

    public void setBootstrap(HttpServer bootstrap) {
        this.bootstrap = bootstrap;
    }

    public void setParam1(int param1) {
        this.param1 = param1;
    }

    public void setParam2(int[] param2) {
        this.param2 = param2;
    }

    @After
    public void destroy() {
        bootstrap.shutdown();
    }
}
