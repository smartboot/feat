package tech.smartboot.feat.demo;

import tech.smartboot.feat.Feat;

public class HelloWorld {
    public static void main(String[] args) {
        Feat.httpServer()
                .httpHandler(request -> request.getResponse().write("Hello World"))
                .listen(8081);
    }
}
