/*******************************************************************************
 * Copyright (c) 2024, tech.smartboot. All rights reserved.
 * project name: feat
 * file name: HttpsDemo.java
 * Date: 2022-02-04
 * Author: sandao (zhengjunweimail@163.com)
 ******************************************************************************/

package tech.smartboot.feat.demo;

import org.smartboot.socket.extension.plugins.SslPlugin;
import org.smartboot.socket.extension.plugins.StreamMonitorPlugin;
import org.smartboot.socket.extension.ssl.factory.PemServerSSLContextFactory;
import tech.smartboot.feat.core.common.HeaderValue;
import tech.smartboot.feat.core.server.HttpRequest;
import tech.smartboot.feat.core.server.HttpResponse;
import tech.smartboot.feat.core.server.HttpServer;
import tech.smartboot.feat.core.server.PushBuilder;
import tech.smartboot.feat.core.server.handler.BaseHttpHandler;
import tech.smartboot.feat.core.server.upgrade.http2.Http2Upgrade;

import javax.net.ssl.SSLEngine;
import javax.net.ssl.SSLParameters;
import java.io.IOException;
import java.util.function.Consumer;

/**
 * @author 三刀（zhengjunweimail@163.com）
 * @version V1.0 , 2022/2/4
 */
public class Http2Demo {
    public static void main(String[] args) throws Exception {
        HttpServer bootstrap = new HttpServer();
        bootstrap.httpHandler(new BaseHttpHandler() {
            @Override
            public void handle(HttpRequest request) throws IOException {
                request.upgrade(new Http2Upgrade() {
                    @Override
                    public void handle(HttpRequest request) throws Throwable {
                        HttpResponse response = request.getResponse();
                        if (request.getRequestURI().equals("/aa.css")) {
                            response.write("hello feat push<br/>".getBytes());
                        } else {
                            PushBuilder pushBuilder = request.newPushBuilder();
                            if (pushBuilder != null) {
                                request.newPushBuilder().path("/aa.css").addHeader("aa", "bb").method("GET").push();
                            }

                            response.write("<html><head></head><body>hello feat<br/></body></html>".getBytes());

                        }
                    }
                });

            }
        });
//        SslPlugin sslPlugin=new SslPlugin(new ServerSSLContextFactory(HttpsDemo.class.getClassLoader().getResourceAsStream("server.keystore"), "123456", "123456"),ClientAuth.NONE);
        SslPlugin sslPlugin = new SslPlugin(new PemServerSSLContextFactory(Http2Demo.class.getClassLoader().getResourceAsStream("example.org.pem"), Http2Demo.class.getClassLoader().getResourceAsStream("example.org-key.pem")), new Consumer<SSLEngine>() {
            @Override
            public void accept(SSLEngine sslEngine) {
                SSLParameters sslParameters = new SSLParameters();
                sslEngine.setUseClientMode(false);
                sslParameters.setApplicationProtocols(new String[]{HeaderValue.Upgrade.H2});
                sslEngine.setSSLParameters(sslParameters);
                HttpRequest.SSL_ENGINE_THREAD_LOCAL.set(sslEngine);
            }
        });
        bootstrap.options()
                .addPlugin(sslPlugin)
                .addPlugin(new StreamMonitorPlugin<>())
                .debug(true);
        bootstrap.listen(8080);
    }
}
