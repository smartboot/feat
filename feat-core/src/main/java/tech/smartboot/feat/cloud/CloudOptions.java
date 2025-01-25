package tech.smartboot.feat.cloud;

import tech.smartboot.feat.core.server.ServerOptions;

public class CloudOptions extends ServerOptions {
    private String[] packages;

    public String[] getPackages() {
        return packages;
    }

    public CloudOptions setPackages(String... packages) {
        this.packages = packages;
        return this;
    }
}
