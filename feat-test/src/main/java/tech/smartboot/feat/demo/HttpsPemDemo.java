/*******************************************************************************
 * Copyright (c) 2024, tech.smartboot. All rights reserved.
 * project name: feat
 * file name: HttpsDemo.java
 * Date: 2022-02-04
 * Author: sandao (zhengjunweimail@163.com)
 ******************************************************************************/

package tech.smartboot.feat.demo;

import org.smartboot.socket.extension.plugins.SslPlugin;
import org.smartboot.socket.extension.ssl.factory.PemServerSSLContextFactory;
import tech.smartboot.feat.Feat;

import java.io.InputStream;

/**
 * @author 三刀（zhengjunweimail@163.com）
 * @version V1.0 , 2022/2/4
 */
public class HttpsPemDemo {
    public static void main(String[] args) throws Exception {
        InputStream certPem = HttpsPemDemo.class.getClassLoader().getResourceAsStream("example.org.pem");
        InputStream keyPem = HttpsPemDemo.class.getClassLoader().getResourceAsStream("example.org-key.pem");
        SslPlugin sslPlugin = new SslPlugin(new PemServerSSLContextFactory(certPem, keyPem));
        Feat.httpServer(opt -> opt.addPlugin(sslPlugin)).httpHandler(req -> {
            req.getResponse().write("Hello Feat Https");
        }).listen();
    }
}
