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

/**
 * @author 三刀(zhengjunweimail@163.com)
 * @version v1.0.0
 */
public class RouterDemo1 {
    public static void main(String[] args) {
        Router router = new Router();
        router
                .route("/", (ctx) -> {
                    ctx.Response.write("root: " + ctx.Request.getRequestURI());
                })
                .route("/*", (ctx) -> {
                    ctx.Response.write("rootPattern: " + ctx.Request.getRequestURI());
                }).route("/a/b/*", (ctx) -> {
                    ctx.Response.write("/a/b/* pattern: " + ctx.Request.getRequestURI());
                });
        router.route("/route1", (ctx) -> {
                    ctx.Response.write("route1: " + ctx.Request.getRequestURI());
                }).route("/route2", (ctx) -> {
                    ctx.Response.write("route2: " + ctx.Request.getRequestURI());
                }).route("/route3/:id", ctx -> ctx.Response.write(ctx.pathParam("id")))
                .route("/route4/:key/:value", (ctx) -> {
                    ctx.Response.write(ctx.pathParam("key") + ":" + ctx.pathParam("value"));
                });

        router.route("/route5", (ctx) -> {
            ctx.Response.write("route5: " + ctx.Request.getMethod());
        });
        router.route("/route5", "GET", (ctx) -> {
            ctx.Response.write("route5 get: " + ctx.Request.getMethod());
        });

        router.route("/route5", "POST", (ctx) -> {
            ctx.Response.write("route5 post: " + ctx.Request.getMethod());
        });
        Feat.httpServer().httpHandler(router).listen();
    }
}
