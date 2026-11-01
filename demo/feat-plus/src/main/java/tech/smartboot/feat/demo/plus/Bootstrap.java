package tech.smartboot.feat.demo.plus;

import java.io.IOException;

import tech.smartboot.feat.cloud.FeatCloud;
import tech.smartboot.feat.core.common.exception.FeatException;
import tech.smartboot.feat.demo.plus.common.SpaFallbackHandler;
import tech.smartboot.feat.router.Router;

public final class Bootstrap {
    private Bootstrap() {
    }

    public static void main(String[] args) {
        FeatCloud.cloudServer(options -> {
            try {
                options.setRouter(new Router(new SpaFallbackHandler()));
            } catch (IOException e) {
                throw new FeatException("初始化静态资源失败", e);
            }
        }).listen();
    }
}
