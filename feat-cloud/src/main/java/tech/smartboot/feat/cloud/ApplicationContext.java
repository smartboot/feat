/*
 *  Copyright (C) [2024] smartboot [zhengjunweimail@163.com]
 *
 *  企业用户未经smartboot组织特别许可，需遵循AGPL-3.0开源协议合理合法使用本项目。
 *
 *   Enterprise users are required to use this project reasonably
 *   and legally in accordance with the AGPL-3.0 open source agreement
 *  without special permission from the smartboot organization.
 */

package tech.smartboot.feat.cloud;

import tech.smartboot.feat.core.common.logging.Logger;
import tech.smartboot.feat.core.common.logging.LoggerFactory;
import tech.smartboot.feat.fileserver.FileServerOptions;
import tech.smartboot.feat.fileserver.HttpStaticResourceHandler;
import tech.smartboot.feat.router.Router;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.ServiceLoader;

/**
 * @author 三刀 zhengjunweimail@163.com
 * @version v1.0.0
 */
public class ApplicationContext {
    private static final Logger LOGGER = LoggerFactory.getLogger(ApplicationContext.class);
    private Map<String, Object> namedBeans = new HashMap<>();

    private final Router router = new Router(new HttpStaticResourceHandler(new FileServerOptions().baseDir("classpath:static")));
    private final CloudOptions options;

    private final List<CloudService> services = new ArrayList<>();

    public ApplicationContext(CloudOptions options) {
        this.options = options;
    }

    public CloudOptions getOptions() {
        return options;
    }

    public void start() throws Throwable {
        for (CloudService service : ServiceLoader.load(CloudService.class)) {
            services.add(service);
        }
        for (CloudService service : services) {
            service.loadBean(this);
        }

        for (CloudService service : services) {
            service.autowired(this);
        }
        for (CloudService service : services) {
            service.postConstruct(this);
        }
        if (options.devMode()) {
            System.out.println("\u001B[32mFeat Router:\u001B[0m");
        }
        for (CloudService service : services) {
            service.router(this, router);
        }
        //释放内存
        namedBeans = null;
    }

    public void addBean(String name, Object object) {
        if (namedBeans.containsKey(name)) {
            throw new IllegalStateException("duplicated name[" + name + "] for " + object.getClass().getName());
        }
        if (LOGGER.isDebugEnabled()) {
            LOGGER.debug("add bean:{} for class:{}", name, object);
        }
        namedBeans.put(name, object);
    }

    public void destroy() {
        for (CloudService aptLoader : services) {
            try {
                aptLoader.destroy();
            } catch (Throwable e) {
                LOGGER.error("error destroying apt loader", e);
            }
        }
    }

    public Router getRouter() {
        return router;
    }

    public <T> T getBean(String name) {
        return (T) namedBeans.get(name);
    }
}
