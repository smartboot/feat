package tech.smartboot.feat.demo.benchmark;

import io.vertx.core.AbstractVerticle;
import io.vertx.core.Vertx;
import io.vertx.core.json.JsonObject;
import io.vertx.ext.web.Router;

public class VertxApp extends AbstractVerticle {

    public static void main(String[] args) {
        Vertx vertx = Vertx.vertx();
        vertx.deployVerticle(new VertxApp());
    }

    @Override
    public void start() {
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

        vertx.createHttpServer()
             .requestHandler(router)
             .listen(8081);
    }
} 