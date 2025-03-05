/*
 *  Copyright (C) [2024] smartboot [zhengjunweimail@163.com]
 *
 *  企业用户未经smartboot组织特别许可，需遵循AGPL-3.0开源协议合理合法使用本项目。
 *
 *   Enterprise users are required to use this project reasonably
 *   and legally in accordance with the AGPL-3.0 open source agreement
 *  without special permission from the smartboot organization.
 */

package tech.smartboot.feat.fileserver;

import tech.smartboot.feat.Feat;
import tech.smartboot.feat.core.client.HttpClient;
import tech.smartboot.feat.core.client.HttpRest;
import tech.smartboot.feat.core.common.io.FeatOutputStream;
import tech.smartboot.feat.core.server.HttpRequest;
import tech.smartboot.feat.core.server.HttpResponse;
import tech.smartboot.feat.core.server.impl.AbstractResponse;
import tech.smartboot.feat.core.server.impl.HttpEndpoint;
import tech.smartboot.feat.core.server.impl.Upgrade;
import tech.smartboot.feat.router.Context;
import tech.smartboot.feat.router.RouterHandler;

import java.io.IOException;
import java.nio.ByteBuffer;

/**
 * @author 三刀(zhengjunweimail @ 163.com)
 * @version v1.0 3/5/25
 */
public class ProxyHandler implements RouterHandler {
    private final ProxyOptions.ProxyRule rule;
    private final HttpClient featClient;

    public ProxyHandler(ProxyOptions.ProxyRule rule) {
        this.rule = rule;
        featClient = Feat.httpClient(rule.getUpstream(), opt -> {
            opt.debug(true);
        });
    }


    @Override
    public void handle(Context ctx) throws Throwable {

        HttpEndpoint endpoint = (HttpEndpoint) ctx.Request;
        HttpRest rest = Feat.httpClient(rule.getUpstream(), opt -> {
//            opt.debug(true);
        }).rest(endpoint.getMethod(), endpoint.getRequestURI());


        endpoint.upgrade(new Upgrade() {

            @Override
            public void init(HttpRequest request, HttpResponse response) {
                // 转发请求头
                request.getHeaderNames().forEach(name -> {
                    if (!name.equalsIgnoreCase("host")) {
                        request.getHeaders(name).forEach(value -> rest.header().add(name, value));
                    }
                });

                rest.onResponseHeader(httpResponse -> {
                    ctx.Response.getOutputStream().reset();
                    //后端的响应头
                    ctx.Response.setHttpStatus(httpResponse.statusCode(), httpResponse.getReasonPhrase());
                    httpResponse.getHeaderNames().forEach(name -> {
                        httpResponse.getHeaders(name).forEach(value -> {
                            ctx.Response.addHeader(name, value);
                        });
                    });
                    //在oneResponseHeader之中注册onResponseBody,绕过Client自身的gzip解压逻辑
                    rest.onResponseBody((rsp, bytes, end) -> ctx.Response.write(bytes));
                }).onSuccess(rsp -> {
                    finishResponse(endpoint);
                }).onFailure(throwable -> {
                    throwable.printStackTrace();
                });
            }

            @Override
            public void onBodyStream(ByteBuffer buffer) {
                if (!buffer.hasRemaining()) {
                    rest.body().flush();
                    return;
                }
                request.getAioSession().awaitRead();
                System.out.println("aa");
                rest.body().transferFrom(buffer, requestBody -> {
                    System.out.println("bb");
//                    requestBody.flush();
                    request.getAioSession().signalRead();
                });
            }

            @Override
            public void destroy() {
                finishResponse(request);
            }
        });
    }

    private void finishResponse(HttpEndpoint abstractRequest) {
        AbstractResponse response = abstractRequest.getResponse();
        //关闭本次请求的输出流
        FeatOutputStream bufferOutputStream = response.getOutputStream();
        if (!bufferOutputStream.isClosed()) {
            try {
                bufferOutputStream.close();
            } catch (IOException e) {
                throw new RuntimeException(e);
            }
        }
        abstractRequest.reset();
        abstractRequest.getAioSession().writeBuffer().flush();
    }
}
