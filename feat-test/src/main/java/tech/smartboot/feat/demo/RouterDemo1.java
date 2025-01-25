package tech.smartboot.feat.demo;

import tech.smartboot.feat.Feat;
import tech.smartboot.feat.router.Router;

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
        Feat.createHttpServer().httpHandler(router).listen();
    }
}
