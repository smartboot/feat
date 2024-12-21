/*******************************************************************************
 * Copyright (c) 2017-2022, org.smartboot. All rights reserved.
 * project name: smart-http
 * file name: HttpsDemo.java
 * Date: 2022-02-04
 * Author: sandao (zhengjunweimail@163.com)
 ******************************************************************************/

package tech.smartboot.feat.demo;

import tech.smartboot.feat.server.HttpBootstrap;
import tech.smartboot.feat.server.HttpRequest;
import tech.smartboot.feat.server.HttpResponse;
import tech.smartboot.feat.server.HttpServerHandler;
import tech.smartboot.feat.server.PushBuilder;
import org.smartboot.socket.extension.plugins.SslPlugin;
import org.smartboot.socket.extension.plugins.StreamMonitorPlugin;
import org.smartboot.socket.extension.ssl.factory.PemServerSSLContextFactory;

import javax.net.ssl.SSLEngine;
import javax.net.ssl.SSLParameters;
import java.io.IOException;
import java.util.function.Consumer;

/**
 * @author 三刀（zhengjunweimail@163.com）
 * @version V1.0 , 2022/2/4
 */
public class HttpsDemo {
    public static void main(String[] args) throws Exception {
        HttpBootstrap bootstrap = new HttpBootstrap();
        bootstrap.httpHandler(new HttpServerHandler() {
            @Override
            public void handle(HttpRequest request, HttpResponse response) throws IOException {
                if (request.getRequestURI().equals("/aa.css")) {
                    response.write("hello smart-http push<br/>".getBytes());
                } else {
                    PushBuilder pushBuilder = request.newPushBuilder();
                    if (pushBuilder != null) {
                        request.newPushBuilder().path("/aa.css").addHeader("aa", "bb").method("GET").push();
                    }

                    response.write("<html><head></head><body>hello smart-http<br/></body></html>".getBytes());

                }
            }
        });
//        SslPlugin sslPlugin=new SslPlugin(new ServerSSLContextFactory(HttpsDemo.class.getClassLoader().getResourceAsStream("server.keystore"), "123456", "123456"),ClientAuth.NONE);
        SslPlugin sslPlugin = new SslPlugin(new PemServerSSLContextFactory(HttpsDemo.class.getClassLoader().getResourceAsStream("example.org.pem"), HttpsDemo.class.getClassLoader().getResourceAsStream("example.org-key.pem")), new Consumer<SSLEngine>() {
            @Override
            public void accept(SSLEngine sslEngine) {
                SSLParameters sslParameters = new SSLParameters();
                sslEngine.setUseClientMode(false);
                sslParameters.setApplicationProtocols(new String[]{"h2"});
                sslEngine.setSSLParameters(sslParameters);
            }
        });
        bootstrap.configuration()
                .addPlugin(sslPlugin)
                .addPlugin(new StreamMonitorPlugin<>())
                .debug(true);
        bootstrap.setPort(8080).start();
    }
}
