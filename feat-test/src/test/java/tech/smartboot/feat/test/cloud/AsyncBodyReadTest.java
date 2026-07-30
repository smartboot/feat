package tech.smartboot.feat.test.cloud;

import com.alibaba.fastjson2.JSONObject;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import tech.smartboot.feat.cloud.FeatCloud;
import tech.smartboot.feat.cloud.annotation.Controller;
import tech.smartboot.feat.cloud.annotation.Param;
import tech.smartboot.feat.cloud.annotation.RequestMapping;
import tech.smartboot.feat.core.client.HttpClient;
import tech.smartboot.feat.core.server.HttpRequest;
import tech.smartboot.feat.test.BastTest;

import java.util.concurrent.ExecutionException;

@Controller("/asyncBody")
public class AsyncBodyReadTest extends BastTest {

    @RequestMapping(value = "/small")
    public String small(HttpRequest request,@Param("object") String object) {
        return request.getContentLength() + "";
    }

    @RequestMapping(value = "/big")
    public String big(HttpRequest request,@Param("object") String object) {
        return request.getContentLength() + "";
    }

    @Before
    public void init() {
        FeatCloud.cloudServer(opts -> opts.setPackages(AsyncBodyReadTest.class.getName()).readBufferSize(1024).debug(true)).listen(8080);
    }

    @Test
    public void test() throws ExecutionException, InterruptedException {
        HttpClient httpClient = getHttpClient();
        JSONObject jsonObject = new JSONObject();
        for (int i = 0; i < 10; i++) {
            jsonObject.put("key" + i, "value" + i);
        }
        int length = jsonObject.toString().getBytes().length;
        Assert.assertEquals(httpClient.post("/asyncBody/small").postJson(jsonObject).submit().get().body(), length+"");

        for (int i = 0; i < 1024; i++) {
            jsonObject.put("key" + i, "value" + i);
        }
        length = jsonObject.toString().getBytes().length;
        Assert.assertEquals(httpClient.post("/asyncBody/big").postJson(jsonObject).submit().get().body(), length+"");
    }
}
