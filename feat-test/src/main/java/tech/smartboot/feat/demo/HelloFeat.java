package tech.smartboot.feat.demo;

import tech.smartboot.feat.Feat;

public class HelloFeat {
    public static void main(String[] args) {
        Feat.httpServer().listen();
    }
}
