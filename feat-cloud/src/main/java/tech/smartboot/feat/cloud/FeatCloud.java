/*
 *  Copyright (C) [2024] smartboot [zhengjunweimail@163.com]
 *
 *  企业用户未经smartboot组织特别许可，需遵循AGPL-3.0开源协议合理合法使用本项目。
 *
 *   Enterprise users are required to use this project reasonably
 *   and legally in accordance with the AGPL-3.0 open source agreement
 *  without special permission from the smartboot organization.
 */

package tech.smartboot.feat.cloud;

import tech.smartboot.feat.Feat;
import tech.smartboot.feat.core.common.exception.FeatException;
import tech.smartboot.feat.core.server.HttpServer;

import java.util.function.Consumer;

public class FeatCloud {

    public static HttpServer cloudServer() {
        return cloudServer(opts -> {
        });
    }

    public static HttpServer cloudServer(Consumer<CloudOptions> options) {
        CloudOptions opt = new CloudOptions();
        options.accept(opt);
        opt.serverName("feat-cloud");
        ApplicationContext application = new ApplicationContext(opt);
        opt.getExternalBeans().forEach(application::addBean);
        try {
            application.start();
        } catch (Throwable e) {
            throw new FeatException("application start exception", e);
        }

        HttpServer server = Feat.httpServer(opt);
        Runnable shutdownHook = server.options().shutdownHook();
        if (shutdownHook != null) {
            server.options().shutdownHook(() -> {
                application.destroy();
                shutdownHook.run();
            });
        } else {
            server.options().shutdownHook(application::destroy);
        }
        server.httpHandler(application.getRouter());
        return server;
    }
}
