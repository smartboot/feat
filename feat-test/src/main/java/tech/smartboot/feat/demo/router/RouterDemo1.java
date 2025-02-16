package tech.smartboot.feat.demo.router;

import tech.smartboot.feat.Feat;
import tech.smartboot.feat.router.Context;
import tech.smartboot.feat.router.Router;
import tech.smartboot.feat.router.RouterHandler;

import java.io.IOException;

public class RouterDemo1 {
    public static void main(String[] args) {
        Router router = new Router();
        router.route("/", (request) -> {
            request.getResponse().write("root: " + request.getRequestURI());
        });
        router.route("/route1", (request) -> {
            request.getResponse().write("route1: " + request.getRequestURI());
        }).route("/route2", (request) -> {
            request.getResponse().write("route2: " + request.getRequestURI());
        });

        router.route("/route3/:id", new RouterHandler() {
            @Override
            public void handle(Context ctx) throws IOException {
                ctx.getResponse().write(ctx.getPathParam("id"));
            }
        });
        Feat.httpServer().httpHandler(router).listen();
    }
}
