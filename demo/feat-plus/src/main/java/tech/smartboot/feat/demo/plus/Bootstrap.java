package tech.smartboot.feat.demo.plus;

import tech.smartboot.feat.cloud.FeatCloud;

public final class Bootstrap {
    private Bootstrap() {
    }

    public static void main(String[] args) {
        FeatCloud.cloudServer().listen();
    }
}
