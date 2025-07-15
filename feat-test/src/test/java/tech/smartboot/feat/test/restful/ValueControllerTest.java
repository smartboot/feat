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

import java.util.List;

/**
 * @author 三刀 zhengjunweimail@163.com
 * @version v1.0 5/24/25
 */
@Controller("values")
public class ValueControllerTest extends BastTest {
    private HttpServer bootstrap;

    @Value
    private int param1;

    @Value
    private int[] param2;

    @Value
    private int[] array;

    @Value("${array}")
    private List<Integer> arrayList;

    @Value
    private String[] array2;

    @Value("${array2}")
    private List<String> array2List;


    @RequestMapping("/test")
    public String param1() {
        Assert.assertEquals(100, param1);
        Assert.assertArrayEquals(new int[]{100, 200, 300}, param2);
        Assert.assertArrayEquals(new int[]{1, 2, 3}, array);
        for (int i = 0; i < array.length; i++) {
            Assert.assertEquals(array[i] + "", arrayList.get(i) + "");
        }
        Assert.assertArrayEquals(new String[]{"a", "b", "c"}, array2);
        for (int i = 0; i < array2.length; i++) {
            Assert.assertEquals(array2[i], array2List.get(i));
        }

        return "ok";
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
        Assert.assertEquals(httpClient.post("/values/test").submit().get().body(), "ok");
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

    public void setArray(int[] array) {
        this.array = array;
    }

    public void setArrayList(List<Integer> arrayList) {
        this.arrayList = arrayList;
    }

    public void setArray2(String[] array2) {
        this.array2 = array2;
    }

    public void setArray2List(List<String> array2List) {
        this.array2List = array2List;
    }

    @After
    public void destroy() {
        bootstrap.shutdown();
    }
}
