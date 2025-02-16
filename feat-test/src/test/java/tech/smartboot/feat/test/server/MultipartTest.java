package tech.smartboot.feat.test.server;

import com.alibaba.fastjson2.JSONArray;
import com.alibaba.fastjson2.JSONObject;
import org.junit.After;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import tech.smartboot.feat.core.client.HttpClient;
import tech.smartboot.feat.core.common.multipart.Part;
import tech.smartboot.feat.core.server.HttpRequest;
import tech.smartboot.feat.core.server.HttpServer;
import tech.smartboot.feat.core.server.handler.BaseHttpHandler;
import tech.smartboot.feat.router.Router;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Future;

/**
 * @Description: TODO
 * @Author MiSinG
 * @Date 2024/8/3
 * @Version V1.0
 **/
public class MultipartTest {

    private HttpServer bootstrap;

    @Before
    public void init() {
        bootstrap = new HttpServer();
        bootstrap.options().debug(true);
        Router routeHandle = new Router();
        routeHandle.route("/formdata", new BaseHttpHandler() {
            @Override
            public void handle(HttpRequest request) throws IOException {
                try {
                    JSONObject jsonObject = new JSONObject();
                    int i = 0;
                    for (Part part : request.getParts()) {
                        String name = part.getName();
                        JSONObject jsonObject2 = new JSONObject();
                        InputStream inputStream = part.getInputStream();
                        JSONArray headers = new JSONArray();
                        for (String headerName : part.getHeaderNames()) {
                            JSONObject j = new JSONObject();
                            j.put("name", headerName);
                            j.put("value", part.getHeader(headerName));
                            headers.add(j);
                        }
                        jsonObject2.put("header", headers);
                        // 非文件处理
                        int contentLength = 0;
                        ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
                        byte[] buffer = new byte[1024];
                        int bytesRead;

                        while ((bytesRead = inputStream.read(buffer)) != -1) {
                            if (part.getSubmittedFileName() == null) outputStream.write(buffer, 0, bytesRead);
                            contentLength += bytesRead;
                        }

                        if (part.getSubmittedFileName() != null) {
                            // 文件处理
                            jsonObject2.put("filename", part.getSubmittedFileName());
                            part.delete();
                        } else {
                            String value = outputStream.toString();
                            jsonObject2.put("value", value);
                        }

                        // 添加公共字段
                        jsonObject2.put("fieldName", name);
                        jsonObject2.put("contentLength", contentLength);
                        jsonObject.put(++i + "", jsonObject2);
                    }

                    request.getResponse().write(jsonObject.toJSONString().getBytes());
                } catch (Exception e) {
                    e.printStackTrace();
                    throw new RuntimeException(e);
                }
            }
        });
        bootstrap.httpHandler(routeHandle).listen(8080);
    }

    @Test
    public void testFormDataRequest() throws InterruptedException, ExecutionException {
        HttpClient client = new HttpClient("127.0.0.1", 8080);
        String body =
                "------WebKitFormBoundary7MA4YWxkTrZu0gW\r\n" +
                        "Content-Disposition: form-data; name=\"field\"\r\n" +
                        "\r\n" +
                        "value1\r\n" +
                        "------WebKitFormBoundary7MA4YWxkTrZu0gW\r\n" +
                        "Content-Disposition: form-data; name=\"file\"; filename=\"example.txt\"\r\n" +
                        "Content-Type: text/plain\r\n" +
                        "\r\n" +
                        "This is the content of the file.\r\n" +
                        "------WebKitFormBoundary7MA4YWxkTrZu0gW--\r\n";
        client.options().debug(true);
        Future<tech.smartboot.feat.core.client.HttpResponse> future = client.post("/formdata")
                .header(h->h.keepalive(true).setContentLength(body.getBytes().length).setContentType("multipart/form-data; boundary=----WebKitFormBoundary7MA4YWxkTrZu0gW"))
                .body(b->b.write(body.getBytes()))
                .onSuccess(response -> {
                    JSONObject jsonObject = JSONObject.parseObject(response.body());
                    System.out.println("response: " + jsonObject);
                })
                .onFailure(t -> {
                    System.out.println(t.getMessage());
                }).submit();
        JSONObject jsonObject = JSONObject.parseObject(future.get().body());
        Assert.assertEquals("value1", jsonObject.getJSONObject("1").getString("value"));
        Assert.assertEquals("example.txt", jsonObject.getJSONObject("2").getString("filename"));

    }

    @Test
    public void testFakeBoundary() throws InterruptedException, ExecutionException {
        HttpClient client = new HttpClient("127.0.0.1", 8080);
        String body =
                "------WebKitFormBoundary7MA4YWxkTrZu0gW\r\n" +
                        "Content-Disposition: form-data; name=\"FiLe1\"; filename=\"testFILE.txt\"\r\n" +
                        "Content-Type: text/plain\r\n" +
                        "\r\n" +
                        "This is the content of the encoded file.\r\n" +
                        "------WebKitFormBoundary7MA4YWxkTr11111\r\n" +
                        "------WebKitFormBoundary7MA4YWxkTrZu0gW\r\n" +
                        "Content-Disposition: form-data; name=\"FiLe2\"; filename=\"testFILE.txt\"\r\n" +
                        "Content-Type: text/plain\r\n" +
                        "\r\n" +
                        "This is the content of the encoded file.\r\n" +
                        "------WebKitFormBoundary7MA4YWxkTr11111--\r\n" +
                        "------WebKitFormBoundary7MA4YWxkTrZu0gW--\r\n";

        client.options().debug(true);
        Future<tech.smartboot.feat.core.client.HttpResponse> future = client.post("/formdata")
                .header(h->h.keepalive(true).setContentLength(body.getBytes().length).setContentType("multipart/form-data; boundary=----WebKitFormBoundary7MA4YWxkTrZu0gW"))
                .body(b->b.write(body.getBytes()))
                .onSuccess(response -> {
                    JSONObject jsonObject = JSONObject.parseObject(response.body());
                    System.out.println("jsonObject = " + jsonObject);
                })
                .onFailure(t -> System.out.println(t.getMessage()))
                .submit();
        JSONObject jsonObject = JSONObject.parseObject(future.get().body());
        Assert.assertEquals("FiLe1", jsonObject.getJSONObject("1").getString("fieldName"));
        Assert.assertEquals("testFILE.txt", jsonObject.getJSONObject("1").getString("filename"));
        Assert.assertEquals("FiLe2", jsonObject.getJSONObject("2").getString("fieldName"));
        Assert.assertEquals("testFILE.txt", jsonObject.getJSONObject("2").getString("filename"));

    }

    @Test
    public void testEmptyFormData() throws InterruptedException, ExecutionException {
        HttpClient client = new HttpClient("127.0.0.1", 8080);
        String body = "------WebKitFormBoundary7MA4YWxkTrZu0gW--\r\n";
        client.options().debug(true);
        Future<tech.smartboot.feat.core.client.HttpResponse> future = client.post("/formdata")
                .header(h->h.keepalive(true).setContentLength(body.getBytes().length).setContentType("multipart/form-data; boundary=----WebKitFormBoundary7MA4YWxkTrZu0gW"))
                .body(b->b.write(body.getBytes()))
                .onSuccess(response -> {
                    System.out.println("jsonObject = " + JSONObject.parseObject(response.body()));
                })
                .onFailure(t -> System.out.println(t.getMessage()))
                .submit();
        JSONObject jsonObject = JSONObject.parseObject(future.get().body());
        Assert.assertTrue(jsonObject.isEmpty());

    }

    /**
     * 测试大文件
     */
    @Test
    public void testFormDataWithLargeFile() throws InterruptedException, ExecutionException {
        HttpClient client = new HttpClient("127.0.0.1", 8080);
        StringBuilder largeContent = new StringBuilder();
        for (int i = 0; i < 10000; i++) {
            largeContent.append("This is line ").append(i).append(" of the large file.\n");
        }
        String body =
                "------WebKitFormBoundary7MA4YWxkTrZu0gW\r\n" +
                        "Content-Disposition: form-data; name=\"largeFile\"; filename=\"largefile.txt\"\r\n" +
                        "Content-Type: text/plain\r\n" +
                        "\r\n" +
                        largeContent + "\r\n" +
                        "------WebKitFormBoundary7MA4YWxkTrZu0gW--\r\n";
        client.options().debug(false);
        Future<tech.smartboot.feat.core.client.HttpResponse> future = client.post("/formdata")
                .header(h->h.keepalive(true).setContentLength(body.getBytes().length).setContentType("multipart/form-data; boundary=----WebKitFormBoundary7MA4YWxkTrZu0gW"))
                .body(b->b.write(body.getBytes()))
                .onSuccess(response -> {
                    JSONObject jsonObject = JSONObject.parseObject(response.body());
                    System.out.println("jsonObject = " + jsonObject);
                })
                .onFailure(t -> System.out.println(t.getMessage()))
                .submit();
        JSONObject jsonObject = JSONObject.parseObject(future.get().body());
        Assert.assertEquals("largefile.txt", jsonObject.getJSONObject("1").getString("filename"));
        Assert.assertEquals("368890", jsonObject.getJSONObject("1").getString("contentLength"));

    }

    /**
     * 测试带有编码后的参数
     */
    @Test
    public void testFormDataWithEncodedParameter() throws InterruptedException, ExecutionException {
        HttpClient client = new HttpClient("127.0.0.1", 8080);
        String body =
                "------WebKitFormBoundary7MA4YWxkTrZu0gW\r\n" +
                        "Content-Disposition: form-data; name=\"field\"; filename*=us-ascii'en-us'This%20is%20%2A%2A%2Afun%2A%2A%2A\r\n" +
                        "Content-Type: text/plain\r\n" +
                        "\r\n" +
                        "This is the content of the encoded file.\r\n" +
                        "------WebKitFormBoundary7MA4YWxkTrZu0gW--\r\n";
        client.options().debug(false);
        Future<tech.smartboot.feat.core.client.HttpResponse> future = client.post("/formdata")
                .header(h->h.keepalive(true).setContentLength(body.getBytes().length).setContentType("multipart/form-data; boundary=----WebKitFormBoundary7MA4YWxkTrZu0gW"))
                .body(b->b.write(body.getBytes()))
                .onSuccess(response -> {
                    JSONObject jsonObject = JSONObject.parseObject(response.body());
                    System.out.println("jsonObject = " + jsonObject);
                })
                .onFailure(t -> System.out.println(t.getMessage()))
                .submit();
        JSONObject jsonObject = JSONObject.parseObject(future.get().body());
        Assert.assertEquals("This is ***fun***", jsonObject.getJSONObject("1").getString("filename"));

    }

    /**
     * 没有选择文件时，上传空文件
     */
    @Test
    public void testFormDataWithTextInEmptyFileField() throws InterruptedException, ExecutionException {
        HttpClient client = new HttpClient("127.0.0.1", 8080);
        String body =
                "------WebKitFormBoundary7MA4YWxkTrZu0gW\r\n" +
                        "Content-Disposition: form-data; name=\"file\"; filename=\"\"\r\n" +
                        "\r\n" +
                        "\r\n" +
                        "------WebKitFormBoundary7MA4YWxkTrZu0gW--\r\n";
        client.options().debug(false);
        Future<tech.smartboot.feat.core.client.HttpResponse> future = client.post("/formdata")
                .header(h->h.keepalive(true).setContentLength(body.getBytes().length).setContentType("multipart/form-data; boundary=----WebKitFormBoundary7MA4YWxkTrZu0gW"))
                .body(b->b.write(body.getBytes()))
                .onSuccess(response -> {
                    JSONObject jsonObject = JSONObject.parseObject(response.body());
                    System.out.println("jsonObject = " + jsonObject);
                })
                .onFailure(t -> System.out.println(t.getMessage()))
                .submit();
        JSONObject jsonObject = JSONObject.parseObject(future.get().body());
        Assert.assertEquals("0", jsonObject.getJSONObject("1").getString("contentLength"));
        Assert.assertEquals("", jsonObject.getJSONObject("1").getString("filename"));

    }

    /**
     * 测试折叠头部
     */
    @Test
    public void testFormDataWithFoldedHeaders() throws InterruptedException, ExecutionException {
        HttpClient client = new HttpClient("127.0.0.1", 8080);
        String body =
                "------WebKitFormBoundary7MA4YWxkTrZu0gW\r\n" +
                        "Content-Disposition: form-data; name=\"file\"; filename=\"bar.txt\"\r\n" +
                        "Content-Type: text/plain\r\n" +
                        "\r\n" +
                        "This is some content in the file\r\n" + "\r\n" +
                        "------WebKitFormBoundary7MA4YWxkTrZu0gW\r\n" +
                        "Content-Disposition: form-data; name=\"textField\"\r\n" +
                        "\r\n" +
                        "textValue\r\n" +
                        "------WebKitFormBoundary7MA4YWxkTrZu0gW\r\n" +
                        "Content-Disposition: form-data; name=\"multiField\"\r\n" +
                        "\r\n" +
                        "multiValue1\r\n" +
                        "------WebKitFormBoundary7MA4YWxkTrZu0gW\r\n" +
                        "Content-Disposition: form-data; name=\"multiField\"\r\n" +
                        "\r\n" +
                        "multiValue2\r\n" +
                        "------WebKitFormBoundary7MA4YWxkTrZu0gW--\r\n";
        client.options().debug(false);
        Future<tech.smartboot.feat.core.client.HttpResponse> future = client.post("/formdata")
                .header(h->h.keepalive(true).setContentLength(body.getBytes().length).setContentType("multipart/form-data; boundary=----WebKitFormBoundary7MA4YWxkTrZu0gW"))
                .body(b->b.write(body.getBytes()))
                .onSuccess(response -> {
                    JSONObject jsonObject = JSONObject.parseObject(response.body());
                    System.out.println("jsonObject = " + jsonObject);
                })
                .onFailure(t -> System.out.println(t.getMessage()))
                .submit();
        JSONObject jsonObject = JSONObject.parseObject(future.get().body());
        // 验证 file 字段
        Assert.assertEquals("bar.txt", jsonObject.getJSONObject("1").getString("filename"));

        // 验证 textField 字段
        Assert.assertEquals("textValue", jsonObject.getJSONObject("2").getString("value"));

        // 验证 multiField 字段
        Assert.assertEquals("multiValue1", jsonObject.getJSONObject("3").getString("value"));
        Assert.assertEquals("multiValue2", jsonObject.getJSONObject("4").getString("value"));

    }

    @Test
    public void testCaseSensitivity() throws InterruptedException, ExecutionException {
        HttpClient client = new HttpClient("127.0.0.1", 8080);
        String body =
                "------WebKitFormBoundary7MA4YWxkTrZu0gW\r\n" +
                        "Content-Disposition: form-data; name=\"FiLe\"; filename=\"testFILE.txt\"\r\n" +
                        "Content-Type: text/plain\r\n" +
                        "\r\n" +
                        "This is the content of the encoded file.\r\n" +
                        "------WebKitFormBoundary7MA4YWxkTrZu0gW--\r\n";
        client.options().debug(true);
        Future<tech.smartboot.feat.core.client.HttpResponse> future = client.post("/formdata")
                .header(h->h.keepalive(true).setContentLength(body.getBytes().length).setContentType("multipart/form-data; boundary=----WebKitFormBoundary7MA4YWxkTrZu0gW"))
                .body(b->b.write(body.getBytes()))
                .onSuccess(response -> {
                    JSONObject jsonObject = JSONObject.parseObject(response.body());
                    System.out.println("jsonObject = " + jsonObject);
                })
                .onFailure(t -> System.out.println(t.getMessage()))
                .submit();
        JSONObject jsonObject = JSONObject.parseObject(future.get().body());
        Assert.assertEquals("FiLe", jsonObject.getJSONObject("1").getString("fieldName"));
        Assert.assertEquals("testFILE.txt", jsonObject.getJSONObject("1").getString("filename"));

    }

    @Test
    public void testCRLFInContent() throws InterruptedException, ExecutionException {
        HttpClient client = new HttpClient("127.0.0.1", 8080);
        String body =
                "------WebKitFormBoundary7MA4YWxkTrZu0gW\r\n" +
                        "Content-Disposition: form-data; name=\"field\"\r\n" +
                        "\r\n" +
                        "This is the content.\r\n\r\n" +
                        "------WebKitFormBoundary7MA4YWxkTrZu0gW--\r\n";
        client.options().debug(true);
        Future<tech.smartboot.feat.core.client.HttpResponse> future = client.post("/formdata")
                .header(h->h.keepalive(true).setContentLength(body.getBytes().length).setContentType("multipart/form-data; boundary=----WebKitFormBoundary7MA4YWxkTrZu0gW"))
                .body(b->b.write(body.getBytes()))
                .onSuccess(response -> {
                    JSONObject jsonObject = JSONObject.parseObject(response.body());
                    System.out.println("jsonObject = " + jsonObject);
                })
                .onFailure(t -> System.out.println(t.getMessage()))
                .submit();
        JSONObject jsonObject = JSONObject.parseObject(future.get().body());
        Assert.assertEquals("field", jsonObject.getJSONObject("1").getString("fieldName"));

    }


    @After
    public void destroy() {
        bootstrap.shutdown();
    }

}
