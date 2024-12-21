/*******************************************************************************
 * Copyright (c) 2024, tech.smartboot. All rights reserved.
 * project name: feat
 * file name: SimpleSmartHttp.java
 * Date: 2021-06-08
 * Author: sandao (zhengjunweimail@163.com)
 ******************************************************************************/

package tech.smartboot.feat.demo;

import tech.smartboot.feat.core.server.HttpServer;
import tech.smartboot.feat.core.server.HttpRequest;
import tech.smartboot.feat.core.server.HttpResponse;
import tech.smartboot.feat.core.server.HttpServerHandler;

import java.io.IOException;
import java.io.InputStream;


public class SimpleSmartHttp {
    public static void main(String[] args) {
        HttpServer bootstrap = new HttpServer();
        bootstrap.configuration().debug(true);
        bootstrap.httpHandler(new HttpServerHandler() {
            @Override
            public void handle(HttpRequest request, HttpResponse response) throws IOException {
                InputStream in = request.getInputStream();
                byte[] b = new byte[1024];
                int i;
                while ((i = in.read(b)) > 0) {
                    System.out.println(new String(b, 0, i));
                }
                response.write("hello feat<br/>".getBytes());
            }
        }).setPort(8080).start();
    }
}