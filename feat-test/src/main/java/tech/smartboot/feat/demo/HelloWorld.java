package tech.smartboot.feat.demo;

import tech.smartboot.feat.core.Feat;

public class HelloWorld {
    public static void main(String[] args) {
        Feat.createHttpServer()
                .httpHandler(request -> request.getResponse().write("Hello World"))
                .listen(8081);
    }
}
