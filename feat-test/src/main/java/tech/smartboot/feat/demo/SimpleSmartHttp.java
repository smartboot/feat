/*******************************************************************************
 * Copyright (c) 2024, tech.smartboot. All rights reserved.
 * project name: feat
 * file name: SimpleSmartHttp.java
 * Date: 2021-06-08
 * Author: sandao (zhengjunweimail@163.com)
 ******************************************************************************/

package tech.smartboot.feat.demo;

import tech.smartboot.feat.Feat;

import java.io.InputStream;


public class SimpleSmartHttp {
    public static void main(String[] args) {
        Feat.createHttpServer(options -> options.debug(true))
                .httpHandler(request -> {
                    InputStream in = request.getInputStream();
                    byte[] b = new byte[1024];
                    int i;
                    while ((i = in.read(b)) > 0) {
                        System.out.println(new String(b, 0, i));
                    }
                    request.getResponse().write("hello feat<br/>".getBytes());
                }).listen(8080);
    }
}