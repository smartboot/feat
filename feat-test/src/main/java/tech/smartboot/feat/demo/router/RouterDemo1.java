package tech.smartboot.feat.demo.router;

import tech.smartboot.feat.Feat;
import tech.smartboot.feat.router.Router;

public class RouterDemo1 {
    public static void main(String[] args) {
        Router router = new Router();
        router.http("/", (request) -> {
            request.getResponse().write("root: " + request.getRequestURI());
        });
        router.http("/route1", (request) -> {
            request.getResponse().write("route1: " + request.getRequestURI());
        }).http("/route2", (request) -> {
            request.getResponse().write("route2: " + request.getRequestURI());
        });

        router.route("/route3/:id", ctx -> ctx.getResponse().write(ctx.getPathParam("id")))
                .route("/route4/:key", (ctx) -> {
                    ctx.getResponse().write(ctx.getPathParam("key") + ":" + ctx.getPathParam("value"));
                });
        Feat.httpServer().httpHandler(router).listen();
    }
}
