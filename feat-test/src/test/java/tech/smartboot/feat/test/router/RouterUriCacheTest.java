/*
 *  Copyright (C) [2024] smartboot [zhengjunweimail@163.com]
 *
 *  企业用户未经smartboot组织特别许可，需遵循Apache-2.0开源协议合理合法使用本项目。
 *
 *   Enterprise users are required to use this project reasonably
 *   and legally in accordance with the Apache-2.0 open source agreement
 *  without special permission from the smartboot organization.
 */

package tech.smartboot.feat.test.router;

import org.junit.After;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import tech.smartboot.feat.core.client.HttpClient;
import tech.smartboot.feat.core.client.HttpResponse;
import tech.smartboot.feat.core.common.HttpStatus;
import tech.smartboot.feat.core.server.HttpServer;
import tech.smartboot.feat.router.Router;
import tech.smartboot.feat.test.BastTest;

import java.util.concurrent.ExecutionException;

/**
 * 路由URI缓存防污染测试：请求流量不再写入uriByteTree缓存，
 * 超长URI（>32字节）不得截断入树导致32字节前缀路由被别名劫持
 *
 * @author 三刀 zhengjunweimail@163.com
 * @version v1.0.0
 */
public class RouterUriCacheTest extends BastTest {
    private static final String ROUTE_32 = "/0123456789012345678901234567890";
    private static final String LONG_33 = "/01234567890123456789012345678901";

    private HttpServer bootstrap;

    @Before
    public void init() {
        Assert.assertEquals(32, ROUTE_32.getBytes().length);
        Assert.assertEquals(33, LONG_33.getBytes().length);
        bootstrap = new HttpServer();
        Router router = new Router();
        router.route(ROUTE_32, ctx -> ctx.Request.getResponse().write("hit"));
        bootstrap.httpHandler(router);
        bootstrap.listen(SERVER_PORT);
    }

    @Test
    public void testLongUriDoesNotPolluteRoute() throws ExecutionException, InterruptedException {
        HttpClient httpClient = getHttpClient();

        //先请求33字节URI（旧实现会将其截断缓存到深度32节点，污染前缀路由）
        Assert.assertEquals(HttpStatus.NOT_FOUND.value(), httpClient.get(LONG_33).submit().get().statusCode());

        //再请求32字节前缀URI，必须仍命中注册的路由而非被缓存劫持
        HttpResponse response = httpClient.get(ROUTE_32).submit().get();
        Assert.assertEquals(HttpStatus.OK.value(), response.statusCode());
        Assert.assertEquals("hit", response.body());

        //重复大量随机URI请求后路由依旧正确（运行期流量不再写入缓存）
        for (int i = 0; i < 100; i++) {
            httpClient.get("/random/" + i).submit().get();
        }
        response = httpClient.get(ROUTE_32).submit().get();
        Assert.assertEquals(HttpStatus.OK.value(), response.statusCode());
        Assert.assertEquals("hit", response.body());
    }

    @After
    public void destroy() {
        bootstrap.shutdown();
    }
}
