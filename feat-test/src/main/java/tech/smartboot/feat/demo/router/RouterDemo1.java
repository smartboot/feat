package tech.smartboot.feat.demo.router;

import tech.smartboot.feat.Feat;
import tech.smartboot.feat.router.Router;

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
        Feat.httpServer().httpHandler(router).listen();
    }
}
