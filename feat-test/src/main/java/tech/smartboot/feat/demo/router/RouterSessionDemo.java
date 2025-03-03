/*
 *  Copyright (C) [2024] smartboot [zhengjunweimail@163.com]
 *
 *  企业用户未经smartboot组织特别许可，需遵循AGPL-3.0开源协议合理合法使用本项目。
 *
 *   Enterprise users are required to use this project reasonably
 *   and legally in accordance with the AGPL-3.0 open source agreement
 *  without special permission from the smartboot organization.
 */

package tech.smartboot.feat.demo.router;

import tech.smartboot.feat.Feat;
import tech.smartboot.feat.router.Router;
import tech.smartboot.feat.router.session.Session;

/**
 * @author 三刀(zhengjunweimail @ 163.com)
 * @version v1.0 3/3/25
 */
public class RouterSessionDemo {
    public static void main(String[] args) {
        Router router = new Router();
        router.addInterceptor("/session/*", (context, completableFuture, chain) -> {
                    Session session = context.session();
                    Object o = session.get("flag");
                    if (o == null) {
                        context.Response.write("session is null");
                    } else {
                        chain.proceed(context, completableFuture);
                    }
                })
                .route("/", (ctx) -> {
                    ctx.Response.write("root: " + ctx.Request.getRequestURI());
                })
                .route("/createSession", (ctx) -> {
                    ctx.Response.write("createSession: " + ctx.session().getSessionId());
                    ctx.session().put("flag","aa");
                })
                .route("/session/check", (ctx) -> {
                    ctx.Response.write("sessionId: " + ctx.session().getSessionId());
                }).route("/session/clear", (ctx) -> {
                    ctx.session().invalidate();
                });
        Feat.httpServer(opt -> opt.debug(true)).httpHandler(router).listen();
    }
}
