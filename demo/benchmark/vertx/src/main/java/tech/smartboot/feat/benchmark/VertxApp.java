package tech.smartboot.feat.benchmark;

import io.vertx.core.Future;
import io.vertx.core.VerticleBase;
import io.vertx.core.Vertx;
import io.vertx.core.json.JsonObject;
import io.vertx.ext.web.Router;

public class VertxApp extends VerticleBase {

    public static void main(String[] args) {
        long start = System.currentTimeMillis();
        Vertx vertx = Vertx.vertx();
        vertx.deployVerticle(new VertxApp());
        System.out.println("启动时间：" + (System.currentTimeMillis() - start));
    }

    @Override
    public Future<?> start() {
        Router router = Router.router(vertx);

        router.get("/hello").handler(ctx ->
                ctx.response()
                        .putHeader("content-type", "text/plain")
                        .end("Hello World!")
        );

        router.get("/json").handler(ctx -> {
            JsonObject response = new JsonObject()
                    .put("message", "Hello")
                    .put("value", "World");

            ctx.response()
                    .putHeader("content-type", "application/json")
                    .end(response.encode());
        });

        return vertx.createHttpServer()
                .requestHandler(router)
                .listen(8081);
    }
}
