package tech.smartboot.feat.demo;

import tech.smartboot.feat.core.Feat;

public class HelloFeat {
    public static void main(String[] args) {
        Feat.createHttpServer().listen();
    }
}
