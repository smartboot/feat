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
import org.smartboot.socket.extension.ssl.factory.AutoServerSSLContextFactory;
import tech.smartboot.feat.Feat;
import tech.smartboot.feat.core.server.HttpRequest;

import javax.net.ssl.SSLEngine;
import java.util.function.Consumer;

/**
 * @author 三刀 zhengjunweimail@163.com
 * @version v1.0.0
 */
public class HttpsSSLEngineDemo {
    public static void main(String[] args) throws Exception {
        SslPlugin sslPlugin = new SslPlugin(new AutoServerSSLContextFactory(), (Consumer<SSLEngine>) sslEngine -> {
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
