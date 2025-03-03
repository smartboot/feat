/*
 *  Copyright (C) [2024] smartboot [zhengjunweimail@163.com]
 *
 *  企业用户未经smartboot组织特别许可，需遵循AGPL-3.0开源协议合理合法使用本项目。
 *
 *   Enterprise users are required to use this project reasonably
 *   and legally in accordance with the AGPL-3.0 open source agreement
 *  without special permission from the smartboot organization.
 */

package tech.smartboot.feat.demo;

import org.smartboot.socket.extension.plugins.SslPlugin;
import org.smartboot.socket.extension.ssl.factory.PemServerSSLContextFactory;
import tech.smartboot.feat.Feat;

import java.io.InputStream;

/**
 * @author 三刀(zhengjunweimail@163.com)
 * @version v1.0.0
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
