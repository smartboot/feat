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
import tech.smartboot.feat.core.server.HttpRequest;

import javax.net.ssl.SSLEngine;
import java.io.InputStream;
import java.util.function.Consumer;

/**
 * @author 三刀（zhengjunweimail@163.com）
 * @version V1.0 , 2022/2/4
 */
public class HttpsSSLEngineDemo {
    public static void main(String[] args) throws Exception {
        InputStream certPem = HttpsSSLEngineDemo.class.getClassLoader().getResourceAsStream("example.com+5.pem");
        InputStream keyPem = HttpsSSLEngineDemo.class.getClassLoader().getResourceAsStream("example.com+5-key.pem");
        SslPlugin sslPlugin = new SslPlugin(new PemServerSSLContextFactory(certPem, keyPem), (Consumer<SSLEngine>) sslEngine -> {
//            sslEngine.setUseClientMode(false);
            HttpRequest.SSL_ENGINE_THREAD_LOCAL.set(sslEngine);
        });
        Feat.httpServer(opt -> opt.addPlugin(sslPlugin)).httpHandler(req -> {
            SSLEngine engine = req.getSslEngine();
            if (engine == null) {
                req.getResponse().write("engine is null");
            } else {
                req.getResponse().write("engine=" + engine);
            }
        }).listen();
    }
}
