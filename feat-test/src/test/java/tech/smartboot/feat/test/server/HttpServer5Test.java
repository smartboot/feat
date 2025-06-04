/*
 *  Copyright (C) [2024] smartboot [zhengjunweimail@163.com]
 *
 *  企业用户未经smartboot组织特别许可，需遵循AGPL-3.0开源协议合理合法使用本项目。
 *
 *   Enterprise users are required to use this project reasonably
 *   and legally in accordance with the AGPL-3.0 open source agreement
 *  without special permission from the smartboot organization.
 */

package tech.smartboot.feat.test.server;

import org.junit.After;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import tech.smartboot.feat.core.client.HttpClient;
import tech.smartboot.feat.core.common.HeaderName;
import tech.smartboot.feat.core.common.HeaderValue;
import tech.smartboot.feat.core.common.HttpStatus;
import tech.smartboot.feat.core.server.HttpResponse;
import tech.smartboot.feat.core.server.HttpServer;
import tech.smartboot.feat.test.BastTest;

import java.nio.charset.StandardCharsets;

/**
 * @author 三刀
 * @version v1.0 6/4/25
 */
public class HttpServer5Test extends BastTest {
    HttpClient httpClient;
    HttpServer bootstrap;

    @Before
    public void init() {
        bootstrap = new HttpServer();
        bootstrap.options().debug(true);
        bootstrap.listen(SERVER_PORT);
        httpClient = getHttpClient();
    }

    @After
    public void destroy() {
        httpClient.close();
        bootstrap.shutdown();
    }

    @Test
    public void test1() throws Throwable {
        bootstrap.httpHandler(request -> {
            HttpResponse response = request.getResponse();
            response.setContentType(HeaderValue.ContentType.TEXT_PLAIN_UTF8);
            byte[] bytes = "Hello World".getBytes(StandardCharsets.UTF_8);
            response.setContentLength(bytes.length);
            response.getOutputStream().write(bytes);
        });
        tech.smartboot.feat.core.client.HttpResponse response = httpClient.get("http://localhost:" + SERVER_PORT + "/hello").submit().get();
        Assert.assertEquals(HttpStatus.OK.value(), response.statusCode());
        Assert.assertEquals(HeaderValue.ContentType.TEXT_PLAIN_UTF8, response.getHeader(HeaderName.CONTENT_TYPE));
        Assert.assertEquals("11", response.getHeader(HeaderName.CONTENT_LENGTH));
        Assert.assertEquals("feat", response.getHeader(HeaderName.SERVER));
        Assert.assertEquals(4, response.getHeaderNames().size());
    }

    @Test
    public void test2() throws Throwable {
        bootstrap.httpHandler(request -> {
            HttpResponse response = request.getResponse();
            response.setContentType(HeaderValue.ContentType.TEXT_PLAIN_UTF8);
            response.setHeader("aa", "bb");
            byte[] bytes = "Hello World".getBytes(StandardCharsets.UTF_8);
            response.setContentLength(bytes.length);
            response.getOutputStream().write(bytes);
        });
        tech.smartboot.feat.core.client.HttpResponse response = httpClient.get("http://localhost:" + SERVER_PORT + "/hello").submit().get();
        Assert.assertEquals(HttpStatus.OK.value(), response.statusCode());
        Assert.assertEquals(HeaderValue.ContentType.TEXT_PLAIN_UTF8, response.getHeader(HeaderName.CONTENT_TYPE));
        Assert.assertEquals("11", response.getHeader(HeaderName.CONTENT_LENGTH));
        Assert.assertEquals("feat", response.getHeader(HeaderName.SERVER));
        Assert.assertEquals("bb", response.getHeader("aa"));
        Assert.assertEquals(5, response.getHeaderNames().size());
    }

    @Test
    public void test3() throws Throwable {
        bootstrap.httpHandler(request -> {
            HttpResponse response = request.getResponse();
            response.setContentType(HeaderValue.ContentType.TEXT_PLAIN_UTF8);
            response.setHeader("aa", "bb");
            byte[] bytes = "Hello World".getBytes(StandardCharsets.UTF_8);
            response.getOutputStream().write(bytes);
        });
        tech.smartboot.feat.core.client.HttpResponse response = httpClient.get("http://localhost:" + SERVER_PORT + "/hello").submit().get();
        Assert.assertEquals(HttpStatus.OK.value(), response.statusCode());
        Assert.assertEquals(HeaderValue.ContentType.TEXT_PLAIN_UTF8, response.getHeader(HeaderName.CONTENT_TYPE));
        Assert.assertEquals("chunked", response.getHeader(HeaderName.TRANSFER_ENCODING));
        Assert.assertEquals("feat", response.getHeader(HeaderName.SERVER));
        Assert.assertEquals("bb", response.getHeader("aa"));
        Assert.assertEquals(5, response.getHeaderNames().size());
    }

    @Test
    public void test4() throws Throwable {
        bootstrap.httpHandler(request -> {
            HttpResponse response = request.getResponse();
            response.setContentType(HeaderValue.ContentType.TEXT_PLAIN_UTF8);
            byte[] bytes = "Hello World".getBytes(StandardCharsets.UTF_8);
            response.getOutputStream().write(bytes);
        });
        tech.smartboot.feat.core.client.HttpResponse response = httpClient.get("http://localhost:" + SERVER_PORT + "/hello").submit().get();
        Assert.assertEquals(HttpStatus.OK.value(), response.statusCode());
        Assert.assertEquals(HeaderValue.ContentType.TEXT_PLAIN_UTF8, response.getHeader(HeaderName.CONTENT_TYPE));
        Assert.assertEquals("chunked", response.getHeader(HeaderName.TRANSFER_ENCODING));
        Assert.assertEquals("feat", response.getHeader(HeaderName.SERVER));
        Assert.assertEquals(4, response.getHeaderNames().size());
    }

    @Test
    public void test5() throws Throwable {
        bootstrap.httpHandler(request -> {
            HttpResponse response = request.getResponse();
            response.setContentType(HeaderValue.ContentType.TEXT_PLAIN_UTF8);
            byte[] bytes = "Hello World".getBytes(StandardCharsets.UTF_8);
            response.getOutputStream().write(bytes);
        });
        tech.smartboot.feat.core.client.HttpResponse response = httpClient.rest("HEAD", "http://localhost:" + SERVER_PORT + "/hello").submit().get();
        Assert.assertEquals(HttpStatus.OK.value(), response.statusCode());
        Assert.assertEquals(HeaderValue.ContentType.TEXT_PLAIN_UTF8, response.getHeader(HeaderName.CONTENT_TYPE));
        Assert.assertEquals("feat", response.getHeader(HeaderName.SERVER));
        Assert.assertEquals(3, response.getHeaderNames().size());
    }

    @Test
    public void test6() throws Throwable {
        bootstrap.httpHandler(request -> {
            HttpResponse response = request.getResponse();
            response.setContentType(HeaderValue.ContentType.TEXT_PLAIN_UTF8);
            response.setHeader("aa", "bb");
            byte[] bytes = "Hello World".getBytes(StandardCharsets.UTF_8);
            response.getOutputStream().write(bytes);
        });
        tech.smartboot.feat.core.client.HttpResponse response = httpClient.rest("HEAD", "http://localhost:" + SERVER_PORT + "/hello").submit().get();
        Assert.assertEquals(HttpStatus.OK.value(), response.statusCode());
        Assert.assertEquals(HeaderValue.ContentType.TEXT_PLAIN_UTF8, response.getHeader(HeaderName.CONTENT_TYPE));
        Assert.assertEquals("feat", response.getHeader(HeaderName.SERVER));
        Assert.assertEquals("bb", response.getHeader("aa"));
        Assert.assertEquals(4, response.getHeaderNames().size());
    }


    @Test
    public void test11() throws Throwable {
        bootstrap.httpHandler(request -> {
            HttpResponse response = request.getResponse();
            response.setContentType(HeaderValue.ContentType.APPLICATION_JSON);
            byte[] bytes = "Hello World".getBytes(StandardCharsets.UTF_8);
            response.setContentLength(bytes.length);
            response.getOutputStream().write(bytes);
        });
        tech.smartboot.feat.core.client.HttpResponse response = httpClient.get("http://localhost:" + SERVER_PORT + "/hello").submit().get();
        Assert.assertEquals(HttpStatus.OK.value(), response.statusCode());
        Assert.assertEquals(HeaderValue.ContentType.APPLICATION_JSON, response.getHeader(HeaderName.CONTENT_TYPE));
        Assert.assertEquals("11", response.getHeader(HeaderName.CONTENT_LENGTH));
        Assert.assertEquals("feat", response.getHeader(HeaderName.SERVER));
        Assert.assertEquals(4, response.getHeaderNames().size());
    }

    @Test
    public void test12() throws Throwable {
        bootstrap.httpHandler(request -> {
            HttpResponse response = request.getResponse();
            response.setContentType(HeaderValue.ContentType.APPLICATION_JSON);
            response.setHeader("aa", "bb");
            byte[] bytes = "Hello World".getBytes(StandardCharsets.UTF_8);
            response.setContentLength(bytes.length);
            response.getOutputStream().write(bytes);
        });
        tech.smartboot.feat.core.client.HttpResponse response = httpClient.get("http://localhost:" + SERVER_PORT + "/hello").submit().get();
        Assert.assertEquals(HttpStatus.OK.value(), response.statusCode());
        Assert.assertEquals(HeaderValue.ContentType.APPLICATION_JSON, response.getHeader(HeaderName.CONTENT_TYPE));
        Assert.assertEquals("11", response.getHeader(HeaderName.CONTENT_LENGTH));
        Assert.assertEquals("feat", response.getHeader(HeaderName.SERVER));
        Assert.assertEquals("bb", response.getHeader("aa"));
        Assert.assertEquals(5, response.getHeaderNames().size());
    }

    @Test
    public void test13() throws Throwable {
        bootstrap.httpHandler(request -> {
            HttpResponse response = request.getResponse();
            response.setContentType(HeaderValue.ContentType.APPLICATION_JSON);
            response.setHeader("aa", "bb");
            byte[] bytes = "Hello World".getBytes(StandardCharsets.UTF_8);
            response.getOutputStream().write(bytes);
        });
        tech.smartboot.feat.core.client.HttpResponse response = httpClient.get("http://localhost:" + SERVER_PORT + "/hello").submit().get();
        Assert.assertEquals(HttpStatus.OK.value(), response.statusCode());
        Assert.assertEquals(HeaderValue.ContentType.APPLICATION_JSON, response.getHeader(HeaderName.CONTENT_TYPE));
        Assert.assertEquals("chunked", response.getHeader(HeaderName.TRANSFER_ENCODING));
        Assert.assertEquals("feat", response.getHeader(HeaderName.SERVER));
        Assert.assertEquals("bb", response.getHeader("aa"));
        Assert.assertEquals(5, response.getHeaderNames().size());
    }

    @Test
    public void test14() throws Throwable {
        bootstrap.httpHandler(request -> {
            HttpResponse response = request.getResponse();
            response.setContentType(HeaderValue.ContentType.APPLICATION_JSON);
            byte[] bytes = "Hello World".getBytes(StandardCharsets.UTF_8);
            response.getOutputStream().write(bytes);
        });
        tech.smartboot.feat.core.client.HttpResponse response = httpClient.get("http://localhost:" + SERVER_PORT + "/hello").submit().get();
        Assert.assertEquals(HttpStatus.OK.value(), response.statusCode());
        Assert.assertEquals(HeaderValue.ContentType.APPLICATION_JSON, response.getHeader(HeaderName.CONTENT_TYPE));
        Assert.assertEquals("chunked", response.getHeader(HeaderName.TRANSFER_ENCODING));
        Assert.assertEquals("feat", response.getHeader(HeaderName.SERVER));
        Assert.assertEquals(4, response.getHeaderNames().size());
    }

    @Test
    public void test15() throws Throwable {
        bootstrap.httpHandler(request -> {
            HttpResponse response = request.getResponse();
            response.setContentType(HeaderValue.ContentType.APPLICATION_JSON);
            byte[] bytes = "Hello World".getBytes(StandardCharsets.UTF_8);
            response.getOutputStream().write(bytes);
        });
        tech.smartboot.feat.core.client.HttpResponse response = httpClient.rest("HEAD", "http://localhost:" + SERVER_PORT + "/hello").submit().get();
        Assert.assertEquals(HttpStatus.OK.value(), response.statusCode());
        Assert.assertEquals(HeaderValue.ContentType.APPLICATION_JSON, response.getHeader(HeaderName.CONTENT_TYPE));
        Assert.assertEquals("feat", response.getHeader(HeaderName.SERVER));
        Assert.assertEquals(3, response.getHeaderNames().size());
    }

    @Test
    public void test16() throws Throwable {
        bootstrap.httpHandler(request -> {
            HttpResponse response = request.getResponse();
            response.setContentType(HeaderValue.ContentType.APPLICATION_JSON);
            response.setHeader("aa", "bb");
            byte[] bytes = "Hello World".getBytes(StandardCharsets.UTF_8);
            response.getOutputStream().write(bytes);
        });
        tech.smartboot.feat.core.client.HttpResponse response = httpClient.rest("HEAD", "http://localhost:" + SERVER_PORT + "/hello").submit().get();
        Assert.assertEquals(HttpStatus.OK.value(), response.statusCode());
        Assert.assertEquals(HeaderValue.ContentType.APPLICATION_JSON, response.getHeader(HeaderName.CONTENT_TYPE));
        Assert.assertEquals("feat", response.getHeader(HeaderName.SERVER));
        Assert.assertEquals("bb", response.getHeader("aa"));
        Assert.assertEquals(4, response.getHeaderNames().size());
    }
}
